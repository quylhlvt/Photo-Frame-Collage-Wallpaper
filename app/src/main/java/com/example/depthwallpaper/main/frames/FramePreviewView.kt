package com.example.depthwallpaper.main.frames

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import com.example.depthwallpaper.main.staticphoto.CellAdjust
import kotlin.math.cos
import kotlin.math.sin

/** Phone-shaped (9:19.5) preview of a frame template. */
class FramePreviewView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    private val renderer = FrameRenderer(ctx)
    private val clip = Path()
    private val bounds = RectF()

    var template: FrameTemplate = FrameTemplates.all[0]
        set(v) { field = v; invalidate() }
    var photos: List<Bitmap?> = emptyList()
        set(v) { field = v; invalidate() }
    var bgColor: Int? = null // null: use the template's own background
        set(v) { field = v; invalidate() }
    var bgImage: Bitmap? = null
        set(v) { field = v; invalidate() }
    /** Per-slot zoom/pan, edited in place by gestures. */
    var adjusts: List<CellAdjust> = emptyList()
        set(v) { field = v; invalidate() }
    var selected = -1
        set(v) { field = v; invalidate() }
    var showHint = false
    /** Called on a tap; the index is -1 when the tap hit no card. */
    var onSlotTap: ((Int) -> Unit)? = null
    var onAdjusted: (() -> Unit)? = null

    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private val size = FloatArray(2)
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var downSlot = -1
    private var moved = false
    private var multiTouch = false

    private val scaleDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val i = selected
            val pic = photos.getOrNull(i) ?: return true
            val adj = adjusts.getOrNull(i) ?: return true
            renderer.photoSize(template, i, width.toFloat(), size)
            adj.zoom = (adj.zoom * detector.scaleFactor).coerceIn(1f, 5f)
            renderer.clamp(adj, pic, size[0], size[1])
            invalidate()
            onAdjusted?.invoke()
            return true
        }
    })

    /** Sets the zoom of slot [i] (1..5), keeping the photo inside its window. */
    fun setZoom(i: Int, zoom: Float) {
        val pic = photos.getOrNull(i) ?: return
        val adj = adjusts.getOrNull(i) ?: return
        renderer.photoSize(template, i, width.toFloat(), size)
        adj.zoom = zoom.coerceIn(1f, 5f)
        renderer.clamp(adj, pic, size[0], size[1])
        invalidate()
    }

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
        renderer.draw(canvas, w, h, template, photos, bg = bgImage, hint = showHint, bgColor = bgColor ?: template.defaultBg, adjusts = adjusts, selected = selected)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (onSlotTap == null) return false // read-only list previews let taps reach their parent
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downX = event.x; downY = event.y; lastX = downX; lastY = downY
                moved = false; multiTouch = false
                downSlot = renderer.slotAt(template, width.toFloat(), height.toFloat(), downX, downY)
            }
            MotionEvent.ACTION_POINTER_DOWN -> multiTouch = true
            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector.isInProgress || event.pointerCount > 1) { multiTouch = true; return true }
                if (multiTouch) return true
                if (!moved && Math.hypot((event.x - downX).toDouble(), (event.y - downY).toDouble()) > slop) moved = true
                if (moved && selected >= 0 && downSlot == selected) {
                    val pic = photos.getOrNull(selected)
                    val adj = adjusts.getOrNull(selected)
                    if (pic != null && adj != null) {
                        // The card is tilted: turn the finger movement into the card's own axes.
                        val a = Math.toRadians(-template.slots[selected].rot.toDouble())
                        val dx = event.x - lastX
                        val dy = event.y - lastY
                        val lx = (dx * cos(a) - dy * sin(a)).toFloat()
                        val ly = (dx * sin(a) + dy * cos(a)).toFloat()
                        renderer.photoSize(template, selected, width.toFloat(), size)
                        renderer.pan(adj, pic, size[0], size[1], lx, ly)
                        invalidate()
                        onAdjusted?.invoke()
                    }
                }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP -> if (!moved && !multiTouch) onSlotTap?.invoke(downSlot)
        }
        return true
    }
}
