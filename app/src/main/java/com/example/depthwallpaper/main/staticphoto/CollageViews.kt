package com.example.depthwallpaper.main.staticphoto

import android.content.Context
import com.example.depthwallpaper.core.dp
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.view.View

/**
 * Phone-shaped (9:19.5) live preview of a collage. Tap a frame to select it; with a frame selected,
 * drag to move its photo and pinch to zoom.
 */
class CollagePreviewView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private val renderer = CollageRenderer()
    private val clip = Path()
    private val bounds = RectF()
    private val size = FloatArray(2)
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop

    var layout: CollageLayout = Collages.layoutsFor(1)[0]
        set(v) { field = v; invalidate() }
    var theme: CollageTheme = Collages.themes[0]
        set(v) { field = v; invalidate() }
    var photos: List<Bitmap?> = emptyList()
        set(v) { field = v; invalidate() }
    /** Per-cell zoom/pan; edited in place by gestures. */
    var adjusts: List<CellAdjust> = emptyList()
    var selected = -1
        set(v) { field = v; invalidate() }
    var showHint = false
    /** Called on a tap; the index is -1 when the tap hit no frame. */
    var onCellTap: ((Int) -> Unit)? = null
    var onAdjusted: (() -> Unit)? = null

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var downCell = -1
    private var moved = false
    private var multiTouch = false

    private val scaleDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val i = selected
            val pic = photos.getOrNull(i) ?: return true
            val adj = adjusts.getOrNull(i) ?: return true
            renderer.innerSize(layout, theme, width.toFloat(), height.toFloat(), i, size)
            adj.zoom = (adj.zoom * detector.scaleFactor).coerceIn(1f, 5f)
            renderer.clamp(adj, pic, size[0], size[1])
            invalidate()
            onAdjusted?.invoke()
            return true
        }
    })

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
        renderer.draw(canvas, w, h, layout, theme, photos, showHint, adjusts, selected)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Read-only previews (the DIY template grid) must let taps through to their parent card.
        if (onCellTap == null) return false
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downX = event.x; downY = event.y; lastX = downX; lastY = downY
                moved = false; multiTouch = false
                downCell = renderer.cellAt(layout, width.toFloat(), height.toFloat(), downX, downY)
            }
            MotionEvent.ACTION_POINTER_DOWN -> multiTouch = true
            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector.isInProgress || event.pointerCount > 1) { multiTouch = true; return true }
                if (multiTouch) return true
                if (!moved && Math.hypot((event.x - downX).toDouble(), (event.y - downY).toDouble()) > slop) moved = true
                if (moved && selected >= 0 && downCell == selected) {
                    val pic = photos.getOrNull(selected)
                    val adj = adjusts.getOrNull(selected)
                    if (pic != null && adj != null) {
                        renderer.innerSize(layout, theme, width.toFloat(), height.toFloat(), selected, size)
                        renderer.pan(adj, pic, size[0], size[1], event.x - lastX, event.y - lastY)
                        invalidate()
                        onAdjusted?.invoke()
                    }
                }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP -> if (!moved && !multiTouch) onCellTap?.invoke(downCell)
        }
        return true
    }
}

/** Small outline thumbnail of a layout, like the layout picker in collage apps. */
class LayoutThumbView(ctx: Context, private val layout: CollageLayout) : View(ctx) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF1F4F5.toInt() }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7FA8F8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = context.dp(2f)
    }
    private val r = RectF()
    var chosen = false
        set(v) { field = v; invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = line.strokeWidth
        r.set(pad, pad, w - pad, h - pad)
        canvas.drawRect(r, fill)
        line.color = if (chosen) 0xFF2F6BFF.toInt() else 0xFF7FA8F8.toInt()
        line.strokeWidth = context.dp(if (chosen) 3f else 1.5f)
        val a = if (layout.fullBleed) r else RectF(r.left + r.width() * 0.06f, r.top + r.height() * 0.14f, r.right - r.width() * 0.06f, r.bottom - r.height() * 0.05f)
        for (c in layout.cells) {
            canvas.drawRect(
                a.left + c.left * a.width(), a.top + c.top * a.height(),
                a.left + c.right * a.width(), a.top + c.bottom * a.height(), line,
            )
        }
    }
}
