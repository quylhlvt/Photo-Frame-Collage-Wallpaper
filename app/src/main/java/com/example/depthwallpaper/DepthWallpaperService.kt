package com.example.depthwallpaper

import android.app.KeyguardManager
import android.service.wallpaper.WallpaperService
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Background + clock + a grid of polaroid frames.
 * On the LOCK screen (screen turns on while the keyguard is showing) all polaroids start
 * stacked in the centre and fly out to their grid positions, then stay there.
 * The home screen just shows the final layout.
 */
class DepthWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DepthEngine()

    inner class DepthEngine : Engine() {
        private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val matrix = Matrix()

        private var bg: Bitmap? = null
        private var photo: Bitmap? = null // cropped photo used inside every polaroid
        private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(8f, 0f, 3f, 0x55000000)
        }
        private val rect = RectF()
        private val src = Rect()
        private val dst = RectF()
        private var animator: ValueAnimator? = null
        private var progress = 1f // 1 = final layout (resting), 0 = all stacked in the centre
        private var lastPopAt = 0L

        private val screenOnReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (isOnLockScreen()) playPop()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            bg = decode(R.drawable.wp_bg)
            photo = buildPhoto()
            setTouchEventsEnabled(true) // only used in the preview screen, see onTouchEvent
            ContextCompat.registerReceiver(
                applicationContext, screenOnReceiver,
                IntentFilter(Intent.ACTION_SCREEN_ON),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (!visible) {
                animator?.cancel()
                progress = 1f // show the final layout when not animating
                return
            }
            // Lock screen -> play the effect. Home screen -> just draw the resting state.
            if (!isPreview && isOnLockScreen()) playPop() else draw()
        }

        private fun isOnLockScreen(): Boolean {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            return km.isKeyguardLocked
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = draw()

        override fun onTouchEvent(event: MotionEvent) {
            if (isPreview && event.actionMasked == MotionEvent.ACTION_DOWN) playPop(force = true)
        }

        private fun playPop(force: Boolean = false) {
            val now = SystemClock.elapsedRealtime()
            // SCREEN_ON and onVisibilityChanged often fire together: play once.
            if (!force && now - lastPopAt < DEBOUNCE_MS) return
            lastPopAt = now
            animator?.cancel()
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = POP_DURATION_MS
                interpolator = LinearInterpolator()
                addUpdateListener { progress = it.animatedValue as Float; draw() }
                start()
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            val canvas = holder.lockCanvas() ?: return
            try {
                canvas.drawColor(Color.BLACK)
                bg?.let { drawBackground(canvas, it) }
                drawClock(canvas)
                drawPolaroids(canvas)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun drawBackground(canvas: Canvas, bmp: Bitmap) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val base = max(w / bmp.width, h / bmp.height) // centerCrop
            matrix.reset()
            matrix.postScale(base, base)
            matrix.postTranslate((w - bmp.width * base) / 2f, (h - bmp.height * base) / 2f)
            canvas.drawBitmap(bmp, matrix, bitmapPaint)
        }

        private fun drawPolaroids(canvas: Canvas) {
            val pic = photo ?: return
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val cardW = w * CARD_W
            val photoAspect = pic.height.toFloat() / pic.width
            val border = cardW * 0.07f
            val photoW = cardW - 2 * border
            val photoH = photoW * photoAspect
            val cardH = border + photoH + cardW * 0.20f
            val cx0 = w * 0.5f
            val cy0 = h * CENTER_Y
            val n = SLOTS.size
            for (i in 0 until n) {
                // each card starts a bit later, all finish together
                val t = ((progress - i * STAGGER) / (1f - (n - 1) * STAGGER)).coerceIn(0f, 1f)
                val e = 1f - (1f - t) * (1f - t) * (1f - t) // ease-out
                val slot = SLOTS[i]
                val cx = cx0 + (w * slot[0] - cx0) * e
                val cy = cy0 + (h * slot[1] - cy0) * e
                val rot = slot[2] * e
                val sc = 1.15f + (1f - 1.15f) * e // slightly bigger while stacked

                val save = canvas.save()
                canvas.translate(cx, cy)
                canvas.rotate(rot)
                canvas.scale(sc, sc)
                rect.set(-cardW / 2f, -cardH / 2f, cardW / 2f, cardH / 2f)
                canvas.drawRoundRect(rect, 4f, 4f, cardPaint)
                src.set(0, 0, pic.width, pic.height)
                dst.set(
                    -cardW / 2f + border, -cardH / 2f + border,
                    cardW / 2f - border, -cardH / 2f + border + photoH,
                )
                canvas.drawBitmap(pic, src, dst, bitmapPaint)
                canvas.restoreToCount(save)
            }
        }

        private fun drawClock(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            clockPaint.textSize = w * 0.30f
            canvas.drawText(timeFormat.format(Date()), w / 2f, h * 0.20f, clockPaint)
        }

        /** Composite background + subject, then crop around the subject to use as the photo. */
        private fun buildPhoto(): Bitmap? {
            val b = bg ?: return null
            val s = decode(R.drawable.wp_subject) ?: return null
            val full = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(full)
            c.drawBitmap(b, null, Rect(0, 0, b.width, b.height), bitmapPaint)
            c.drawBitmap(s, null, Rect(0, 0, b.width, b.height), bitmapPaint)
            s.recycle()
            val x0 = (b.width * 0.20f).toInt()
            val x1 = (b.width * 0.80f).toInt()
            val y0 = (b.height * 0.27f).toInt()
            val y1 = min(b.height, (b.height * 0.57f).toInt())
            val out = Bitmap.createBitmap(full, x0, y0, x1 - x0, y1 - y0)
            if (out !== full) full.recycle()
            return out
        }

        private fun decode(@DrawableRes id: Int): Bitmap? {
            val dm = resources.displayMetrics
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(resources, id, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= dm.widthPixels &&
                bounds.outHeight / (sample * 2) >= dm.heightPixels
            ) sample *= 2
            return BitmapFactory.decodeResource(
                resources, id, BitmapFactory.Options().apply { inSampleSize = sample },
            )
        }

        override fun onDestroy() {
            animator?.cancel()
            runCatching { applicationContext.unregisterReceiver(screenOnReceiver) }
            bg?.recycle()
            photo?.recycle()
            super.onDestroy()
        }
    }

    private companion object {
        const val POP_DURATION_MS = 1400L
        const val DEBOUNCE_MS = 1500L
        const val CARD_W = 0.25f   // polaroid width as a fraction of screen width
        const val CENTER_Y = 0.62f // where the stack starts (fraction of screen height)
        const val STAGGER = 0.05f
        // x (fraction of w), y (fraction of h), rotation in degrees
        val SLOTS = arrayOf(
            floatArrayOf(0.22f, 0.48f, -6f), floatArrayOf(0.50f, 0.47f, 3f), floatArrayOf(0.78f, 0.48f, -4f),
            floatArrayOf(0.22f, 0.63f, 5f), floatArrayOf(0.50f, 0.62f, -3f), floatArrayOf(0.78f, 0.63f, 6f),
            floatArrayOf(0.22f, 0.78f, -2f), floatArrayOf(0.50f, 0.77f, 4f), floatArrayOf(0.78f, 0.78f, -5f),
        )
    }
}
