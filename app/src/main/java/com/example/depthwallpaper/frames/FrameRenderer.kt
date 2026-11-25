package com.example.depthwallpaper.frames

import android.graphics.Bitmap
import com.example.depthwallpaper.staticphoto.CellAdjust
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/** Draws a frame template with its photos. Shared by the wallpaper and the in-app previews. */
class FrameRenderer(private val ctx: android.content.Context) {
    private val assets = DecorAssets(ctx)

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD9D9D9.toInt() }
    private val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF888888.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0xFF2F80ED.toInt(); strokeWidth = 6f
    }
    private val tmp = FloatArray(2)
    private val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val scriptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("cursive", Typeface.ITALIC)
        textAlign = Paint.Align.CENTER
    }
    private var overlay: Bitmap? = null
    private var backdrop: Bitmap? = null
    private var front: Bitmap? = null
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val matrix = Matrix()
    private val rect = RectF()
    private val dst = RectF()
    private val src = Rect()
    private val path = Path()

    private fun cardW(s: Slot, w: Float) = s.w * w
    private fun border(cw: Float) = cw * 0.07f
    private fun photoInset(t: FrameTemplate, cw: Float) = when (t.style) {
        FrameStyle.CAPTION -> cw * 0.05f
        FrameStyle.THIN_DARK, FrameStyle.THIN_WHITE -> cw * 0.05f
        else -> border(cw)
    }
    /** Height of the white/dark strip under the photo. */
    private fun bottomStrip(t: FrameTemplate, cw: Float) = when (t.style) {
        FrameStyle.CAPTION -> cw * 0.30f
        FrameStyle.DARK -> cw * 0.17f
        FrameStyle.THIN_DARK, FrameStyle.THIN_WHITE -> cw * 0.05f
        else -> cw * 0.20f
    }
    private fun hasBorder(t: FrameTemplate) = when (t.style) {
        FrameStyle.POLAROID, FrameStyle.CAPTION, FrameStyle.DARK, FrameStyle.THIN_DARK, FrameStyle.THIN_WHITE -> true
        else -> false
    }
    private fun cardH(t: FrameTemplate, s: Slot, w: Float): Float {
        val cw = cardW(s, w)
        return if (hasBorder(t)) {
            photoInset(t, cw) + (cw - 2 * photoInset(t, cw)) * s.aspect + bottomStrip(t, cw)
        } else cw * s.aspect
    }

    /** Index of the slot under (x, y), or -1. Rotation is ignored, which is close enough for taps. */
    fun slotAt(t: FrameTemplate, w: Float, h: Float, x: Float, y: Float): Int {
        for (i in t.slots.indices.reversed()) {
            val s = t.slots[i]
            if (abs(x - s.x * w) <= cardW(s, w) / 2f && abs(y - s.y * h) <= cardH(t, s, w) / 2f) return i
        }
        return -1
    }

    // Cards (white frame + photo + shadow) are rendered once into small bitmaps and the
    // background is pre-scaled to the screen, so an animation frame only draws ready-made bitmaps.
    private var cards: List<Bitmap> = emptyList()
    private var cT: FrameTemplate? = null
    private var cPhotos: List<Bitmap?>? = null
    private var cW = 0f
    private var cHint = false
    private var cAdj = 0
    private var bgSrc: Bitmap? = null
    private var bgScaled: Bitmap? = null
    private var bgW = 0
    private var bgH = 0
    private var timeText = ""
    private var timeAt = 0L

    /** Build the caches. Cheap when nothing changed; call it before animating. */
    fun prepare(
        w: Float, h: Float, t: FrameTemplate, photos: List<Bitmap?>, bg: Bitmap?, hint: Boolean = false,
        adjusts: List<CellAdjust> = emptyList(),
    ) {
        val adjKey = adjusts.fold(0) { acc, a -> (acc * 31 + a.zoom.toBits()) * 31 + a.cx.toBits() * 17 + a.cy.toBits() }
        if (!(cT === t && cPhotos === photos && cW == w && cHint == hint && cAdj == adjKey)) {
            cards = t.slots.mapIndexed { i, s ->
                val cw = cardW(s, w)
                val ch = cardH(t, s, w)
                val pad = (cw * 0.06f).coerceAtLeast(6f)
                val bmp = Bitmap.createBitmap((cw + 2 * pad).toInt(), (ch + 2 * pad).toInt(), Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                c.translate(bmp.width / 2f, bmp.height / 2f)
                cardPaint.setShadowLayer(cw * 0.03f, 0f, cw * 0.012f, 0x55000000)
                drawCardContent(c, t, s, photos.getOrNull(i), w, hint, adjusts.getOrNull(i))
                bmp
            }
            cT = t; cPhotos = photos; cW = w; cHint = hint; cAdj = adjKey
            overlay = buildOverlay(t, w, h)
            backdrop = buildBackdrop(t, w, h)
            front = buildFront(t, w, h)
        }
        if (bg == null) {
            bgSrc = null; bgScaled = null
        } else if (bgSrc !== bg || bgW != w.toInt() || bgH != h.toInt()) {
            bgW = w.toInt(); bgH = h.toInt()
            val out = Bitmap.createBitmap(bgW, bgH, Bitmap.Config.ARGB_8888)
            val base = max(w / bg.width, h / bg.height) // centerCrop
            matrix.reset()
            matrix.postScale(base, base)
            matrix.postTranslate((w - bg.width * base) / 2f, (h - bg.height * base) / 2f)
            Canvas(out).drawBitmap(bg, matrix, bitmapPaint)
            bgSrc = bg; bgScaled = out
        }
    }

    /**
     * @param progress 1 = final layout, 0 = every card stacked in the centre.
     * @param bg optional background picture; otherwise [bgColor] is used.
     * @param hint draw a "+" on empty slots.
     */
    fun draw(
        canvas: Canvas, w: Float, h: Float, t: FrameTemplate, photos: List<Bitmap?>,
        progress: Float = 1f, bg: Bitmap? = null, hint: Boolean = false, bgColor: Int = BG_COLOR,
        adjusts: List<CellAdjust> = emptyList(), selected: Int = -1,
    ) {
        prepare(w, h, t, photos, bg, hint, adjusts)
        canvas.drawColor(bgColor)
        bgScaled?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        backdrop?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        overlay?.let { o ->
            overlayPaint.alpha = (255 * progress.coerceIn(0f, 1f)).toInt()
            canvas.drawBitmap(o, 0f, 0f, overlayPaint)
        }
        val light = bg == null && (0.299f * Color.red(bgColor) + 0.587f * Color.green(bgColor) + 0.114f * Color.blue(bgColor)) > 170f
        clockPaint.color = if (light) 0xFF333333.toInt() else Color.WHITE
        val now = System.currentTimeMillis()
        if (now - timeAt > 1000L || timeText.isEmpty()) {
            timeText = timeFormat.format(Date(now)); timeAt = now
        }
        clockPaint.textSize = w * 0.30f
        canvas.drawText(timeText, w / 2f, h * 0.20f, clockPaint)

        val n = t.slots.size
        val stagger = if (n > 1) STAGGER else 0f
        for (i in 0 until n) {
            val s = t.slots[i]
            val card = cards[i]
            val p = ((progress - i * stagger) / (1f - (n - 1) * stagger)).coerceIn(0f, 1f)
            val e = 1f - (1f - p) * (1f - p) * (1f - p) // ease-out
            val cx = w * 0.5f + (w * s.x - w * 0.5f) * e
            val cy = h * START_Y + (h * s.y - h * START_Y) * e
            val sc = 1.15f + (1f - 1.15f) * e
            val save = canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(s.rot * e)
            canvas.scale(sc, sc)
            canvas.drawBitmap(card, -card.width / 2f, -card.height / 2f, bitmapPaint)
            if (i == selected) {
                val half = cardW(s, w) / 2f
                val halfH = cardH(t, s, w) / 2f
                canvas.drawRect(-half, -halfH, half, halfH, selectPaint)
            }
            canvas.restoreToCount(save)
        }
        front?.let { f ->
            overlayPaint.alpha = (255 * progress.coerceIn(0f, 1f)).toInt()
            canvas.drawBitmap(f, 0f, 0f, overlayPaint)
        }
    }

    /** Draws one card centred on the canvas origin. */
    private fun drawCardContent(
        canvas: Canvas, t: FrameTemplate, s: Slot, pic: Bitmap?, w: Float, hint: Boolean, adj: CellAdjust?,
    ) {
        val cw = cardW(s, w)
        val ch = cardH(t, s, w)
        if (hasBorder(t)) {
            rect.set(-cw / 2f, -ch / 2f, cw / 2f, ch / 2f)
            val old = cardPaint.color
            cardPaint.color = if (t.style == FrameStyle.DARK || t.style == FrameStyle.THIN_DARK) 0xFF444444.toInt() else Color.WHITE
            canvas.drawRoundRect(rect, 4f, 4f, cardPaint)
            cardPaint.color = old
            val b = photoInset(t, cw)
            dst.set(-cw / 2f + b, -ch / 2f + b, cw / 2f - b, -ch / 2f + b + (cw - 2 * b) * s.aspect)
        } else {
            dst.set(-cw / 2f, -ch / 2f, cw / 2f, ch / 2f)
        }
        val radius = when (t.style) {
            FrameStyle.TILE -> cw * 0.10f
            FrameStyle.PILL, FrameStyle.CIRCLE -> cw / 2f
            else -> 0f
        }
        val clip = canvas.save()
        if (t.style == FrameStyle.SKEW) {
            val k = dst.height() * 0.22f
            path.reset()
            path.moveTo(dst.left, dst.top)
            path.lineTo(dst.right, dst.top + k)
            path.lineTo(dst.right, dst.bottom)
            path.lineTo(dst.left, dst.bottom - k)
            path.close()
            canvas.clipPath(path)
        } else if (radius > 0f) {
            path.reset()
            path.addRoundRect(dst, radius, radius, Path.Direction.CW)
            canvas.clipPath(path)
        }
        if (pic != null) {
            cropSrc(pic, dst.width(), dst.height(), adj)
            canvas.drawBitmap(pic, src, dst, bitmapPaint)
        } else {
            canvas.drawRect(dst, emptyPaint)
            if (hint) {
                plusPaint.textSize = dst.width() * 0.4f
                canvas.drawText("+", dst.centerX(), dst.centerY() + plusPaint.textSize * 0.35f, plusPaint)
            }
        }
        canvas.restoreToCount(clip)

        if (t.style == FrameStyle.PILL) {
            val stroke = cw * 0.022f
            goldPaint.strokeWidth = stroke
            goldPaint.shader = LinearGradient(
                dst.left, dst.top, dst.right, dst.bottom,
                intArrayOf(0xFFB77B45.toInt(), 0xFFF3D9A0.toInt(), 0xFFB77B45.toInt()), null, Shader.TileMode.CLAMP,
            )
            rect.set(dst.left + stroke / 2f, dst.top + stroke / 2f, dst.right - stroke / 2f, dst.bottom - stroke / 2f)
            canvas.drawRoundRect(rect, rect.width() / 2f, rect.width() / 2f, goldPaint)
        }
        val caption = s.caption
        if (caption != null && (t.style == FrameStyle.CAPTION || t.style == FrameStyle.DARK)) {
            val stripTop = dst.bottom
            val stripH = ch / 2f - stripTop
            scriptPaint.color = if (t.style == FrameStyle.DARK) Color.WHITE else if (t.captionColor != 0) t.captionColor else 0xFF111111.toInt()
            scriptPaint.textSize = stripH * if (t.style == FrameStyle.DARK) 0.62f else 0.46f
            canvas.drawText(caption, 0f, stripTop + stripH / 2f + scriptPaint.textSize * 0.33f, scriptPaint)
        }
    }

    private val decorPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private fun drawDecor(canvas: Canvas, d: Decor, w: Float, h: Float) {
        val bmp = assets.bitmap(d.drawable, (d.w * w).toInt().coerceAtLeast(1)) ?: return
        decorPaint.colorFilter = if (d.tint != 0) android.graphics.PorterDuffColorFilter(d.tint, android.graphics.PorterDuff.Mode.SRC_IN) else null
        val save = canvas.save()
        canvas.translate(d.x * w, d.y * h)
        canvas.rotate(d.rot)
        if (d.flip) canvas.scale(-1f, 1f)
        canvas.drawBitmap(bmp, -bmp.width / 2f, -bmp.height / 2f, decorPaint)
        canvas.restoreToCount(save)
    }

    /** Images and shapes behind the photos: background texture, repeated text, panels. */
    private fun buildBackdrop(t: FrameTemplate, w: Float, h: Float): Bitmap? {
        if (t.bgImage == null && t.bgPattern == null && t.panels.isEmpty()) return null
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        t.bgImage?.let { name ->
            if (t.bgTile) {
                // Tiles keep their pixel size, scaled up on big screens.
                val tile = assets.bitmap(name, (256 * max(1f, w / 720f)).toInt())
                if (tile != null) {
                    val p = Paint().apply { shader = android.graphics.BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
                    c.drawRect(0f, 0f, w, h, p)
                }
            } else {
                assets.bitmapExact(name, w.toInt(), h.toInt())?.let { c.drawBitmap(it, 0f, 0f, null) }
            }
        }
        t.bgPattern?.let { text ->
            scriptPaint.color = t.bgPatternColor
            scriptPaint.textSize = w * 0.085f
            var y = h * 0.055f
            while (y < h) {
                for (col in 0 until 3) c.drawText(text, w * (0.17f + 0.33f * col), y, scriptPaint)
                y += h * 0.0545f
            }
        }
        for (pn in t.panels) {
            cloudPaint.style = Paint.Style.FILL
            cloudPaint.color = pn.color
            rect.set((pn.x - pn.w / 2f) * w, (pn.y - pn.h / 2f) * h, (pn.x + pn.w / 2f) * w, (pn.y + pn.h / 2f) * h)
            c.drawRoundRect(rect, 6f, 6f, cloudPaint)
        }
        return bmp
    }

    /** Stickers marked `front`, drawn over the photos. */
    private fun buildFront(t: FrameTemplate, w: Float, h: Float): Bitmap? {
        if (t.decor.none { it.front }) return null
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        for (d in t.decor) if (d.front) drawDecor(c, d, w, h)
        return bmp
    }

    /** Stickers and handwritten labels, drawn once into a screen-sized bitmap and faded in. */
    private fun buildOverlay(t: FrameTemplate, w: Float, h: Float): Bitmap? {
        if (t.labels.isEmpty() && t.decor.none { !it.front }) return null
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        for (d in t.decor) if (!d.front) drawDecor(c, d, w, h)
        for (l in t.labels) {
            scriptPaint.color = l.color
            scriptPaint.textSize = l.size * w
            val save = c.save()
            c.rotate(l.rot, l.x * w, l.y * h)
            c.drawText(l.text, l.x * w, l.y * h, scriptPaint)
            c.restoreToCount(save)
        }
        return bmp
    }

    /** Size of the photo window of slot [i] in screen units, for gesture maths. */
    fun photoSize(t: FrameTemplate, i: Int, w: Float, out: FloatArray) {
        val s = t.slots[i]
        val cw = cardW(s, w)
        if (hasBorder(t)) {
            val pw = cw - 2 * photoInset(t, cw)
            out[0] = pw; out[1] = pw * s.aspect
        } else {
            out[0] = cw; out[1] = cw * s.aspect
        }
    }

    private fun cropSize(bmp: Bitmap, dw: Float, dh: Float, zoom: Float, out: FloatArray) {
        val sa = bmp.width.toFloat() / bmp.height
        val da = dw / dh
        val cw: Float
        val ch: Float
        if (sa > da) { ch = bmp.height.toFloat(); cw = ch * da } else { cw = bmp.width.toFloat(); ch = cw / da }
        out[0] = cw / zoom; out[1] = ch / zoom
    }

    fun clamp(adj: CellAdjust, bmp: Bitmap, dw: Float, dh: Float) {
        cropSize(bmp, dw, dh, adj.zoom, tmp)
        val hx = tmp[0] / 2f / bmp.width
        val hy = tmp[1] / 2f / bmp.height
        adj.cx = adj.cx.coerceIn(hx, 1f - hx)
        adj.cy = adj.cy.coerceIn(hy, 1f - hy)
    }

    /** Moves the visible part of the photo by a drag of (dx, dy) screen units inside the window. */
    fun pan(adj: CellAdjust, bmp: Bitmap, dw: Float, dh: Float, dx: Float, dy: Float) {
        cropSize(bmp, dw, dh, adj.zoom, tmp)
        adj.cx -= dx * (tmp[0] / dw) / bmp.width
        adj.cy -= dy * (tmp[1] / dh) / bmp.height
        clamp(adj, bmp, dw, dh)
    }

    private fun cropSrc(bmp: Bitmap, dw: Float, dh: Float, adj: CellAdjust?) {
        val a = adj ?: DEFAULT_ADJUST
        cropSize(bmp, dw, dh, a.zoom, tmp)
        val cw = tmp[0]
        val ch = tmp[1]
        val left = (a.cx * bmp.width - cw / 2f).coerceIn(0f, bmp.width - cw)
        val top = (a.cy * bmp.height - ch / 2f).coerceIn(0f, bmp.height - ch)
        src.set(left.toInt(), top.toInt(), (left + cw).toInt(), (top + ch).toInt())
    }

    companion object {
        private val DEFAULT_ADJUST = CellAdjust()
        const val BG_COLOR = 0xFF3A3A3A.toInt()
        const val START_Y = 0.62f
        const val STAGGER = 0.05f
    }
}
