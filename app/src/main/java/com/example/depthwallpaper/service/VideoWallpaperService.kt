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
        private var renderer: VideoGlRenderer? = null
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
            // Raw size and rotation tag of the file, to tell whether this phone applied the tag itself.
            var rawW = 0; var rawH = 0; var tagRot = 0
            val r = android.media.MediaMetadataRetriever()
            try {
                r.setDataSource(file.path)
                rawW = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
                rawH = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
                tagRot = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
            } catch (e: Exception) {
            } finally {
                runCatching { r.release() }
            }
            val fit = store.videoFit
            val gl = runCatching { VideoGlRenderer(surface) }.getOrNull() ?: return
            renderer = gl
            val mp = MediaPlayer()
            try {
                mp.setSurface(gl.videoSurface)
                mp.setDataSource(file.path)
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                mp.setOnVideoSizeChangedListener { _, w, h ->
                    // If the player reports the unrotated size, it ignored the rotation tag: apply it here.
                    val manual = if (tagRot % 180 != 0 && w == rawW && h == rawH && w != h) tagRot else 0
                    gl.configure(w, h, manual, fit)
                }
                mp.setOnPreparedListener {
                    prepared = true
                    if (isVisible) runCatching { it.start() }
                }
                mp.setOnErrorListener { _, _, _ -> release(); true }
                player = mp
                prepared = false
                mp.prepareAsync()
            } catch (e: Exception) {
                runCatching { mp.release() }
                player = null
                release()
            }
        }

        private fun release() {
            runCatching { player?.release() }
            player = null
            prepared = false
            renderer?.let { runCatching { it.release() } }
            renderer = null
        }

        override fun onDestroy() {
            handler.removeCallbacks(releaseLater)
            release()
            super.onDestroy()
        }
    }
}
