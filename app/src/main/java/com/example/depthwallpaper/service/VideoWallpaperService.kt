package com.example.depthwallpaper.service

import com.example.depthwallpaper.core.*
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

/**
 * Video live wallpaper: a looping, muted video scaled to fill the screen.
 * Kept separate from [DepthWallpaperService] so its surface is only ever used by MediaPlayer.
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private val store = WallpaperStore(this@VideoWallpaperService)
        private var player: MediaPlayer? = null
        private var prepared = false
        private var loadedVersion = -1L
        private val handler = Handler(Looper.getMainLooper())
        private val releaseLater = Runnable { release() }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            if (isVisible) start()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            release() // the player is bound to the old surface
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            handler.removeCallbacks(releaseLater)
            if (visible) start() else {
                if (prepared) runCatching { player?.pause() }
                // Hidden for a while (screen off / other app on top): free the decoder.
                handler.postDelayed(releaseLater, 30_000)
            }
        }

        private fun start() {
            // A new video was saved: drop the old player.
            if (store.version != loadedVersion) {
                loadedVersion = store.version
                release()
            }
            player?.let { if (prepared) runCatching { it.start() }; return }
            val file = store.videoFile
            val surface = surfaceHolder.surface
            if (!file.exists() || !surface.isValid) return
            val mp = MediaPlayer()
            try {
                mp.setSurface(surface)
                mp.setDataSource(file.path)
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                mp.setOnPreparedListener {
                    prepared = true
                    runCatching { it.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING) }
                    if (isVisible) runCatching { it.start() }
                }
                mp.setOnErrorListener { _, _, _ -> release(); true }
                player = mp
                prepared = false
                mp.prepareAsync()
            } catch (e: Exception) {
                runCatching { mp.release() }
                player = null
            }
        }

        private fun release() {
            runCatching { player?.release() }
            player = null
            prepared = false
        }

        override fun onDestroy() {
            handler.removeCallbacks(releaseLater)
            release()
            super.onDestroy()
        }
    }
}
