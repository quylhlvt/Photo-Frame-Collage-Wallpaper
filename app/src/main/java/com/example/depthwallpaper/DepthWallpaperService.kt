package com.example.depthwallpaper

import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Shows the frame + photos the user saved in the app (or a sample if nothing is saved yet).
 * On the LOCK screen (screen turns on while the keyguard is showing) all cards start stacked in
 * the centre and fly out to their positions, then stay there. The home screen shows the final layout.
 */
class DepthWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DepthEngine()

    inner class DepthEngine : Engine() {
        private val renderer = FrameRenderer()
        private val store = WallpaperStore(this@DepthWallpaperService)

        private var template: FrameTemplate = FrameTemplates.all[0]
        private var photos: List<Bitmap?> = emptyList()
        private var bg: Bitmap? = null
        private var loadedVersion = -1L

        private var animator: ValueAnimator? = null
        private var progress = 1f // 1 = final layout (resting), 0 = all stacked in the centre
        private var lastPopAt = 0L

        private val screenOnReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (isOnLockScreen()) { reload(); playPop() }
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            reload()
            setTouchEventsEnabled(true) // only used in the preview screen, see onTouchEvent
            ContextCompat.registerReceiver(
                applicationContext, screenOnReceiver,
                IntentFilter(Intent.ACTION_SCREEN_ON),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }

        /** (Re)load what the user saved; falls back to the bundled sample. */
        private fun reload() {
            val v = store.version
            if (v == loadedVersion && photos.isNotEmpty()) return
            loadedVersion = v
            val saved = store.load()
            if (saved != null) {
                template = saved.first
                photos = saved.second
                bg = null
            } else {
                template = FrameTemplates.all[0]
                val sample = SamplePhoto.build(resources, sampleSize())
                photos = List(template.slots.size) { sample }
                bg = decode(R.drawable.wp_bg)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (!visible) {
                animator?.cancel()
                progress = 1f // show the final layout when not animating
                return
            }
            reload()
            // Lock screen -> play the effect. Home screen -> just draw the final layout.
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
                renderer.draw(canvas, canvas.width.toFloat(), canvas.height.toFloat(), template, photos, progress, bg)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun sampleSize(): Int {
            val dm = resources.displayMetrics
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(resources, R.drawable.wp_bg, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= dm.widthPixels &&
                bounds.outHeight / (sample * 2) >= dm.heightPixels
            ) sample *= 2
            return sample
        }

        private fun decode(@DrawableRes id: Int): Bitmap? =
            BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply { inSampleSize = sampleSize() })

        override fun onDestroy() {
            animator?.cancel()
            runCatching { applicationContext.unregisterReceiver(screenOnReceiver) }
            super.onDestroy()
        }
    }

    private companion object {
        const val POP_DURATION_MS = 1400L
        const val DEBOUNCE_MS = 1500L
    }
}
