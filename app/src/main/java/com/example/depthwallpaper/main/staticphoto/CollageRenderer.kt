package com.example.depthwallpaper.main.staticphoto

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader

/** How one photo sits in its cell: [zoom] >= 1 and the centre of the visible part as a fraction of the photo. */
class CellAdjust(var zoom: Float = 1f, var cx: Float = 0.5f, var cy: Float = 0.5f)

/** Draws a static collage (theme background + photos in layout cells). Used for previews and for the saved wallpaper. */
class CollageRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD9D9D9.toInt() }
    private val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF888888.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2F6BFF.toInt()
        style = Paint.Style.STROKE
    }
    private val path = Path()
    private val area = RectF()
    private val cell = RectF()
    private val inner = RectF()
    private val src = Rect()
    private var corner = 0f
    private var border = 0f

    /** Fills [area], [cell] and [inner] (pixels) for cell [i]. */
    private fun place(layout: CollageLayout, theme: CollageTheme, w: Float, h: Float, i: Int) {
        val bleed = layout.fullBleed
        if (bleed) area.set(0f, 0f, w, h) else area.set(w * 0.04f, h * 0.14f, w * 0.96f, h * 0.95f)
        val gap = if (bleed) 0f else theme.gap * w
        corner = if (bleed) 0f else theme.corner * w
        border = if (bleed) 0f else theme.border * w
        val c = layout.cells[i]
        cell.set(
            area.left + c.left * area.width() + gap / 2f, area.top + c.top * area.height() + gap / 2f,
            area.left + c.right * area.width() - gap / 2f, area.top + c.bottom * area.height() - gap / 2f,
        )
        if (border > 0f) inner.set(cell.left + border, cell.top + border, cell.right - border, cell.bottom - border)
        else inner.set(cell)
    }

    fun draw(
        canvas: Canvas, w: Float, h: Float, layout: CollageLayout, theme: CollageTheme,
        photos: List<Bitmap?>, hint: Boolean = false,
        adjusts: List<CellAdjust> = emptyList(), selected: Int = -1,
    ) {
        paint.shader = LinearGradient(0f, 0f, 0f, h, theme.top, theme.bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        for (i in layout.cells.indices) {
            place(layout, theme, w, h, i)
            if (border > 0f) canvas.drawRoundRect(cell, corner, corner, borderPaint)
            val r = if (border > 0f) (corner - border).coerceAtLeast(0f) else corner
            val save = canvas.save()
            path.reset()
            path.addRoundRect(inner, r, r, Path.Direction.CW)
            canvas.clipPath(path)
            val pic = photos.getOrNull(i)
            if (pic != null) {
                crop(pic, inner.width(), inner.height(), adjusts.getOrNull(i))
                canvas.drawBitmap(pic, src, inner, paint)
            } else {
                canvas.drawRect(inner, emptyPaint)
                if (hint) {
                    plusPaint.textSize = minOf(inner.width(), inner.height()) * 0.4f
                    canvas.drawText("+", inner.centerX(), inner.centerY() + plusPaint.textSize * 0.35f, plusPaint)
                }
            }
            canvas.restoreToCount(save)
            if (i == selected) {
                selPaint.strokeWidth = w * 0.008f
                canvas.drawRoundRect(inner, r, r, selPaint)
            }
        }
    }

    /** Index of the cell under (x, y), or -1. */
    fun cellAt(layout: CollageLayout, w: Float, h: Float, x: Float, y: Float): Int {
        val a = if (layout.fullBleed) RectF(0f, 0f, w, h) else RectF(w * 0.04f, h * 0.14f, w * 0.96f, h * 0.95f)
        for (i in layout.cells.indices) {
            val c = layout.cells[i]
            if (x >= a.left + c.left * a.width() && x <= a.left + c.right * a.width() &&
                y >= a.top + c.top * a.height() && y <= a.top + c.bottom * a.height()
            ) return i
        }
        return -1
    }

    /** Pixel size of the photo area of cell [i]: out[0] = width, out[1] = height. */
    fun innerSize(layout: CollageLayout, theme: CollageTheme, w: Float, h: Float, i: Int, out: FloatArray) {
        place(layout, theme, w, h, i)
        out[0] = inner.width(); out[1] = inner.height()
    }

    /** Size of the visible part of [bmp] (in photo pixels) for a cell of dw x dh at [zoom]. */
    private fun cropSize(bmp: Bitmap, dw: Float, dh: Float, zoom: Float, out: FloatArray) {
        val sa = bmp.width.toFloat() / bmp.height
        val da = dw / dh
        var cw: Float
        var ch: Float
        if (sa > da) { ch = bmp.height.toFloat(); cw = ch * da } else { cw = bmp.width.toFloat(); ch = cw / da }
        out[0] = cw / zoom; out[1] = ch / zoom
    }

    private val tmp = FloatArray(2)

    /** Keep the visible window inside the photo. */
    fun clamp(adj: CellAdjust, bmp: Bitmap, dw: Float, dh: Float) {
        cropSize(bmp, dw, dh, adj.zoom, tmp)
        val hx = tmp[0] / 2f / bmp.width
        val hy = tmp[1] / 2f / bmp.height
        adj.cx = adj.cx.coerceIn(hx, 1f - hx)
        adj.cy = adj.cy.coerceIn(hy, 1f - hy)
    }

    /** Move the photo by (dx, dy) screen pixels inside its cell. */
    fun pan(adj: CellAdjust, bmp: Bitmap, dw: Float, dh: Float, dx: Float, dy: Float) {
        cropSize(bmp, dw, dh, adj.zoom, tmp)
        adj.cx -= dx * (tmp[0] / dw) / bmp.width
        adj.cy -= dy * (tmp[1] / dh) / bmp.height
        clamp(adj, bmp, dw, dh)
    }

    private fun crop(bmp: Bitmap, dw: Float, dh: Float, adj: CellAdjust?) {
        val a = adj ?: DEFAULT
        cropSize(bmp, dw, dh, a.zoom, tmp)
        val cw = tmp[0]
        val ch = tmp[1]
        val left = (a.cx * bmp.width - cw / 2f).coerceIn(0f, bmp.width - cw)
        val top = (a.cy * bmp.height - ch / 2f).coerceIn(0f, bmp.height - ch)
        src.set(left.toInt(), top.toInt(), (left + cw).toInt(), (top + ch).toInt())
    }

    private companion object { val DEFAULT = CellAdjust() }
}
