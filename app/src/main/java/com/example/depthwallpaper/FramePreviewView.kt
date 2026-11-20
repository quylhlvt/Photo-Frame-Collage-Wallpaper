package com.example.depthwallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/** Phone-shaped (9:19.5) preview of a frame template. */
class FramePreviewView(ctx: Context) : View(ctx) {
    private val renderer = FrameRenderer()
    private val clip = Path()
    private val bounds = RectF()

    var template: FrameTemplate = FrameTemplates.all[0]
        set(v) { field = v; invalidate() }
    var photos: List<Bitmap?> = emptyList()
        set(v) { field = v; invalidate() }
    var showHint = false
    var onSlotTap: ((Int) -> Unit)? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension((h * 9f / 19.5f).toInt(), h)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        bounds.set(0f, 0f, w, h)
        clip.reset()
        clip.addRoundRect(bounds, w * 0.08f, w * 0.08f, Path.Direction.CW)
        canvas.clipPath(clip)
        renderer.draw(canvas, w, h, template, photos, hint = showHint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tap = onSlotTap ?: return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) return true
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val i = renderer.slotAt(template, width.toFloat(), height.toFloat(), event.x, event.y)
            if (i >= 0) tap(i)
            return true
        }
        return false
    }
}
