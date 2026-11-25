package com.example.depthwallpaper.service

import com.example.depthwallpaper.core.*
import com.example.depthwallpaper.frames.*
import com.example.depthwallpaper.R
import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import kotlin.math.max

/**
 * Image live wallpaper with two modes (chosen in the app):
 *  - STATIC: a single photo.
 *  - FRAMES: frame + photos; on the LOCK screen all cards fly out from the centre and stay.
 * Video lives in its own service ([VideoWallpaperService]) because a surface that was drawn on
 * with a Canvas can't later be handed to a MediaPlayer.
 */
class DepthWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DepthEngine()

    inner class DepthEngine : Engine() {
        private val renderer = FrameRenderer()
        private val store = WallpaperStore(this@DepthWallpaperService)

        private var mode = WallpaperMode.FRAMES
        private var loaded = false
        private var loadedVersion = -1L

        // FRAMES
        private var template: FrameTemplate = FrameTemplates.all[0]
        private var photos: List<Bitmap?> = emptyList()
        private var bg: Bitmap? = null
        private var bgColor = FrameRenderer.BG_COLOR

        // STATIC
        private var staticBmp: Bitmap? = null
        private var staticScaled: Bitmap? = null

        private var animator: ValueAnimator? = null
        private var progress = 1f // 1 = final layout (resting), 0 = all stacked in the centre

        // The frames effect plays once every time the lock screen appears (screen off -> on).
        private var needsPop = !WallpaperUi.lockIgnoresLive()
        private var locked = true // true from SCREEN_OFF until the user unlocks (USER_PRESENT)

        // Phones whose lock screen can't show a live wallpaper: play the effect on the home screen
        // right after unlocking instead. USER_PRESENT isn't sent for swipe-only locks, so poll.
        private val ignoresLock = WallpaperUi.lockIgnoresLive()
        private val handler = android.os.Handler(android.os.Looper.getMainLooper())
        private var unlockWatch: Runnable? = null

        private fun watchUnlock() {
            stopWatchingUnlock()
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            var tries = 0
            val r = object : Runnable {
                override fun run() {
                    if (!km.isKeyguardLocked) { unlockWatch = null; handler.postDelayed({ if (isVisible) play() }, 250); return }
                    if (++tries < 400) handler.postDelayed(this, 150) else unlockWatch = null
                }
            }
            unlockWatch = r
            handler.post(r)
        }

        private fun stopWatchingUnlock() {
            unlockWatch?.let { handler.removeCallbacks(it) }
            unlockWatch = null
        }

        private val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        locked = true
                        stopWatchingUnlock()
                        if (mode == WallpaperMode.FRAMES && !ignoresLock) armPop()
                    }
                    Intent.ACTION_USER_PRESENT -> locked = false
                    Intent.ACTION_SCREEN_ON -> if (mode == WallpaperMode.FRAMES) {
                        if (ignoresLock && !isPreview) watchUnlock()
                        else if (isVisible) tryPop()
                    }
                }
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            reload()
            setTouchEventsEnabled(true) // only used in the preview screen, see onTouchEvent
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            ContextCompat.registerReceiver(applicationContext, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }

        /** (Re)load what the user saved; falls back to the bundled sample frames. */
        private fun reload() {
            val v = store.version
            if (loaded && v == loadedVersion) return
            loaded = true
            loadedVersion = v
            staticBmp = null; staticScaled = null

            mode = store.imageMode
            if (mode == WallpaperMode.STATIC) {
                staticBmp = store.staticFile.takeIf { it.exists() }?.let { decodeFile(it.path) }
                if (staticBmp == null) mode = WallpaperMode.FRAMES
            }
            if (mode == WallpaperMode.FRAMES) loadFrames()
            prepareRenderer()
        }

        private fun loadFrames() {
            val saved = store.load()
            if (saved != null) {
                template = saved.frame
                photos = saved.photos
                bg = saved.bgImage
                bgColor = saved.bgColor
            } else {
                template = FrameTemplates.all[0]
                val sample = SamplePhoto.build(resources, sampleSize())
                photos = List(template.slots.size) { sample }
                bg = decode(R.drawable.wp_bg)
                bgColor = FrameRenderer.BG_COLOR
            }
        }

        private fun prepareRenderer() {
            val f = surfaceHolder.surfaceFrame
            if (f.width() <= 0) return
            val w = f.width()
            val h = f.height()
            when (mode) {
                WallpaperMode.FRAMES -> renderer.prepare(w.toFloat(), h.toFloat(), template, photos, bg)
                WallpaperMode.STATIC -> {
                    val src = staticBmp ?: return
                    if (staticScaled?.width == w && staticScaled?.height == h) return
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val base = max(w.toFloat() / src.width, h.toFloat() / src.height) // centerCrop
                    val m = Matrix().apply {
                        postScale(base, base)
                        postTranslate((w - src.width * base) / 2f, (h - src.height * base) / 2f)
                    }
                    Canvas(out).drawBitmap(src, m, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
                    staticScaled = out
                }
                else -> Unit
            }
        }

        // ---------- FRAMES ----------
        private fun isOnLockScreen(): Boolean {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            return locked || km.isKeyguardLocked
        }

        /** Next time the wallpaper shows on the lock screen, play the effect; start from the stacked pose. */
        private fun armPop() {
            needsPop = true
            animator?.cancel()
            progress = 0f
        }

        private fun tryPop() {
            if (isPreview) { draw(); return }
            if (needsPop && isOnLockScreen()) {
                needsPop = false
                play()
            } else {
                if (!needsPop && animator?.isRunning == true) return // already animating
                if (!isOnLockScreen()) needsPop = false
                animator?.cancel()
                progress = 1f
                draw()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            when {
                !visible -> when (mode) {
                    WallpaperMode.STATIC, WallpaperMode.VIDEO -> Unit
                    // Going away while locked (e.g. screen turned off): next time, play from the start.
                    WallpaperMode.FRAMES ->
                        if (!isPreview && !ignoresLock && isOnLockScreen()) armPop() else { animator?.cancel(); progress = 1f }
                }
                else -> {
                    reload()
                    if (mode == WallpaperMode.FRAMES) tryPop() else draw()
                }
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            prepareRenderer()
            draw()
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (isPreview && mode == WallpaperMode.FRAMES && event.actionMasked == MotionEvent.ACTION_DOWN) play()
        }

        private fun play() {
            animator?.cancel()
            prepareRenderer()
            progress = 0f
            draw() // show the stacked pose immediately, no flash of the final layout
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
                if (mode == WallpaperMode.STATIC) {
                    canvas.drawColor(Color.BLACK)
                    staticScaled?.let { canvas.drawBitmap(it, 0f, 0f, null) }
                } else {
                    renderer.draw(canvas, canvas.width.toFloat(), canvas.height.toFloat(), template, photos, progress, bg, bgColor = bgColor)
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun screenSizeSample(w: Int, h: Int): Int {
            val dm = resources.displayMetrics
            var sample = 1
            while (w / (sample * 2) >= dm.widthPixels && h / (sample * 2) >= dm.heightPixels) sample *= 2
            return sample
        }

        private fun sampleSize(): Int {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(resources, R.drawable.wp_bg, bounds)
            return screenSizeSample(bounds.outWidth, bounds.outHeight)
        }

        private fun decodeFile(path: String): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            return BitmapFactory.decodeFile(
                path, BitmapFactory.Options().apply { inSampleSize = screenSizeSample(bounds.outWidth, bounds.outHeight) },
            )
        }

        private fun decode(@DrawableRes id: Int): Bitmap? =
            BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply { inSampleSize = sampleSize() })

        override fun onDestroy() {
            stopWatchingUnlock()
            animator?.cancel()
            runCatching { applicationContext.unregisterReceiver(screenReceiver) }
            super.onDestroy()
        }
    }

    private companion object {
        const val POP_DURATION_MS = 1400L
    }
}
