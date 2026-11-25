package com.example.depthwallpaper.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import java.io.File

/**
 * Shrinks a picked video before it becomes a wallpaper: H.264, short side ~720p, 30 fps,
 * no audio, at most [MAX_SECONDS] seconds. Must be called from the main thread.
 */
object VideoCompressor {
    const val MAX_SECONDS = 15
    private const val MAX_SHORT_SIDE = 720
    private const val BITRATE = 2_500_000

    /** True when the source is already light enough to use as is. */
    fun isLight(ctx: Context, uri: Uri): Boolean {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, uri)
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: return false
            val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: return false
            val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: return false
            val bps = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLong() ?: return false
            minOf(w, h) <= MAX_SHORT_SIDE && ms <= MAX_SECONDS * 1000L && bps <= 4_000_000L
        } catch (e: Exception) {
            false
        } finally {
            runCatching { r.release() }
        }
    }

    /**
     * @param fit true: keep the whole picture inside a screen-shaped frame (black bars, nothing cut off);
     *            false: fill the screen and crop the edges.
     */
    fun compress(
        ctx: Context, uri: Uri, out: File, fit: Boolean = false,
        onProgress: (Int) -> Unit, onDone: (Boolean) -> Unit,
    ) {
        val main = Handler(Looper.getMainLooper())
        val (w, h) = dims(ctx, uri)
        val targetHeight = if (h >= w) MAX_SHORT_SIDE * 16 / 9 else MAX_SHORT_SIDE
        val dm = ctx.resources.displayMetrics
        val outW = MAX_SHORT_SIDE
        val outH = (outW * dm.heightPixels.toFloat() / dm.widthPixels).toInt() / 2 * 2
        val presentation = if (fit) Presentation.createForWidthAndHeight(outW, outH, Presentation.LAYOUT_SCALE_TO_FIT)
        else Presentation.createForHeight(targetHeight)

        val clip = MediaItem.ClippingConfiguration.Builder().setEndPositionMs(MAX_SECONDS * 1000L).build()
        val item = MediaItem.Builder().setUri(uri).setClippingConfiguration(clip).build()
        val effects = Effects(
            emptyList(),
            listOf(presentation, FrameDropEffect.createDefaultFrameDropEffect(30f)),
        )
        val edited = EditedMediaItem.Builder(item).setRemoveAudio(true).setEffects(effects).build()
        val encoder = DefaultEncoderFactory.Builder(ctx)
            .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(BITRATE).build())
            .build()

        out.delete()
        val transformer = Transformer.Builder(ctx)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setEncoderFactory(encoder)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) = onDone(true)
                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    out.delete(); onDone(false)
                }
            })
            .build()
        transformer.start(edited, out.path)

        val holder = ProgressHolder()
        val poll = object : Runnable {
            override fun run() {
                if (transformer.getProgress(holder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    onProgress(holder.progress)
                    main.postDelayed(this, 300)
                }
            }
        }
        main.post(poll)
    }

    private fun dims(ctx: Context, uri: Uri): Pair<Int, Int> {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, uri)
            var w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            var h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
            if (rot == 90 || rot == 270) { val t = w; w = h; h = t }
            w to h
        } catch (e: Exception) {
            0 to 0
        } finally {
            runCatching { r.release() }
        }
    }
}
