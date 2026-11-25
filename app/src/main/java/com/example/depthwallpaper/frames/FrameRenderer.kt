package com.example.depthwallpaper.frames

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
class FrameRenderer {
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
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val matrix = Matrix()
    private val rect = RectF()
    private val dst = RectF()
    private val src = Rect()
    private val path = Path()

    private fun cardW(s: Slot, w: Float) = s.w * w
    private fun border(cw: Float) = cw * 0.07f
    private fun cardH(t: FrameTemplate, s: Slot, w: Float): Float {
        val cw = cardW(s, w)
        return if (t.style == FrameStyle.POLAROID) {
            border(cw) + (cw - 2 * border(cw)) * s.aspect + cw * 0.20f
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
    private var bgSrc: Bitmap? = null
    private var bgScaled: Bitmap? = null
    private var bgW = 0
    private var bgH = 0
    private var timeText = ""
    private var timeAt = 0L

    /** Build the caches. Cheap when nothing changed; call it before animating. */
    fun prepare(w: Float, h: Float, t: FrameTemplate, photos: List<Bitmap?>, bg: Bitmap?, hint: Boolean = false) {
        if (!(cT === t && cPhotos === photos && cW == w && cHint == hint)) {
            cards = t.slots.mapIndexed { i, s ->
                val cw = cardW(s, w)
                val ch = cardH(t, s, w)
                val pad = (cw * 0.06f).coerceAtLeast(6f)
                val bmp = Bitmap.createBitmap((cw + 2 * pad).toInt(), (ch + 2 * pad).toInt(), Bitmap.Config.ARGB_8888)
                val c = Canvas(bmp)
                c.translate(bmp.width / 2f, bmp.height / 2f)
                cardPaint.setShadowLayer(cw * 0.03f, 0f, cw * 0.012f, 0x55000000)
                drawCardContent(c, t, s, photos.getOrNull(i), w, hint)
                bmp
            }
            cT = t; cPhotos = photos; cW = w; cHint = hint
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
    ) {
        prepare(w, h, t, photos, bg, hint)
        canvas.drawColor(bgColor)
        bgScaled?.let { canvas.drawBitmap(it, 0f, 0f, null) }
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
            canvas.restoreToCount(save)
        }
    }

    /** Draws one card centred on the canvas origin. */
    private fun drawCardContent(
        canvas: Canvas, t: FrameTemplate, s: Slot, pic: Bitmap?, w: Float, hint: Boolean,
    ) {
        val cw = cardW(s, w)
        val ch = cardH(t, s, w)
        if (t.style == FrameStyle.POLAROID) {
            rect.set(-cw / 2f, -ch / 2f, cw / 2f, ch / 2f)
            canvas.drawRoundRect(rect, 4f, 4f, cardPaint)
            val b = border(cw)
            dst.set(-cw / 2f + b, -ch / 2f + b, cw / 2f - b, -ch / 2f + b + (cw - 2 * b) * s.aspect)
        } else {
            dst.set(-cw / 2f, -ch / 2f, cw / 2f, ch / 2f)
        }
        val radius = if (t.style == FrameStyle.TILE) cw * 0.10f else 0f
        val clip = canvas.save()
        if (radius > 0f) {
            path.reset()
            path.addRoundRect(dst, radius, radius, Path.Direction.CW)
            canvas.clipPath(path)
        }
        if (pic != null) {
            cropSrc(pic, dst.width(), dst.height())
            canvas.drawBitmap(pic, src, dst, bitmapPaint)
        } else {
            canvas.drawRect(dst, emptyPaint)
            if (hint) {
                plusPaint.textSize = dst.width() * 0.4f
                canvas.drawText("+", dst.centerX(), dst.centerY() + plusPaint.textSize * 0.35f, plusPaint)
            }
        }
        canvas.restoreToCount(clip)
    }

    private fun cropSrc(bmp: Bitmap, dw: Float, dh: Float) {
        val sa = bmp.width.toFloat() / bmp.height
        val da = dw / dh
        if (sa > da) {
            val nw = (bmp.height * da).toInt()
            val x = (bmp.width - nw) / 2
            src.set(x, 0, x + nw, bmp.height)
        } else {
            val nh = (bmp.width / da).toInt()
            val y = (bmp.height - nh) / 2
            src.set(0, y, bmp.width, y + nh)
        }
    }

    companion object {
        const val BG_COLOR = 0xFF3A3A3A.toInt()
        const val START_Y = 0.62f
        const val STAGGER = 0.05f
    }
}
