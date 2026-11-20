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
import android.graphics.Typeface
import android.os.SystemClock
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.animation.DecelerateInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.sin

/**
 * Two layers (background + cut-out subject) with a clock between them.
 * The pop effect plays only on the LOCK screen (when the screen turns on and the
 * keyguard is showing). The home screen stays static.
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
        private val matrix = Matrix()

        private var bg: Bitmap? = null
        private var subject: Bitmap? = null
        private var animator: ValueAnimator? = null
        private var progress = 1f // 1 = resting state
        private var lastPopAt = 0L

        private val screenOnReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (isOnLockScreen()) playPop()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            bg = decode(R.drawable.wp_bg)
            subject = decode(R.drawable.wp_subject)
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
                progress = 1f // never leave the wallpaper stuck mid-zoom
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
                interpolator = DecelerateInterpolator()
                addUpdateListener { progress = it.animatedValue as Float; draw() }
                start()
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            val canvas = holder.lockCanvas() ?: return
            try {
                canvas.drawColor(Color.BLACK)
                // sin(pi * p): 0 -> 1 -> 0, so layers zoom out then come back
                val pop = sin(Math.PI * progress).toFloat()
                bg?.let { drawLayer(canvas, it, 1f + BG_POP * pop) }
                drawClock(canvas, 1f + CLOCK_POP * pop)
                subject?.let { drawLayer(canvas, it, 1f + SUBJECT_POP * pop) }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun drawLayer(canvas: Canvas, bmp: Bitmap, extra: Float) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val base = max(w / bmp.width, h / bmp.height) // centerCrop
            matrix.reset()
            matrix.postScale(base, base)
            matrix.postTranslate((w - bmp.width * base) / 2f, (h - bmp.height * base) / 2f)
            matrix.postScale(extra, extra, w / 2f, h / 2f)
            canvas.drawBitmap(bmp, matrix, bitmapPaint)
        }

        private fun drawClock(canvas: Canvas, extra: Float) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            clockPaint.textSize = w * 0.30f
            val save = canvas.save()
            canvas.scale(extra, extra, w / 2f, h / 2f)
            canvas.restoreToCount(save)
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
            subject?.recycle()
            super.onDestroy()
        }
    }

    private companion object {
        const val POP_DURATION_MS = 900L
        const val DEBOUNCE_MS = 1500L
        const val BG_POP = 0.06f       // background zooms a little
        const val CLOCK_POP = 0.12f    // clock in between
        const val SUBJECT_POP = 0.22f  // subject zooms the most
    }
}
