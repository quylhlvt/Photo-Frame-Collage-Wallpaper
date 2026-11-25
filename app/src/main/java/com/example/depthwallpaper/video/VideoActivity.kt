package com.example.depthwallpaper.video

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import java.io.File
import android.widget.Toast
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.WallpaperStore
import com.example.depthwallpaper.core.WallpaperUi
import com.example.depthwallpaper.databinding.ActivityVideoBinding
import com.example.depthwallpaper.service.VideoWallpaperService

/** Mode 3: a looping, muted video as the wallpaper. */
class VideoActivity : AbsBaseActivity<ActivityVideoBinding>() {
    private lateinit var store: WallpaperStore
    private var pickedUri: Uri? = null
    private var busy = false

    override fun getLayoutId() = R.layout.activity_video

    override fun initView() {
        store = WallpaperStore(this)
        store.videoFile.takeIf { it.exists() }?.let { f ->
            thumbnail { setDataSource(f.path) }?.let { binding.preview.setImageBitmap(it) }
        }
        loadOnline()
    }

    private fun loadOnline() {
        Thread {
            val list = VideoCatalog.fetch()
            if (list.isNullOrEmpty()) return@Thread
            val thumbs = list.map { VideoCatalog.thumb(it.thumbUrl) }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                binding.onlineTitle.visibility = View.VISIBLE
                binding.onlineScroll.visibility = View.VISIBLE
                list.forEachIndexed { i, v ->
                    val iv = ImageView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(72), dp(128)).apply { marginEnd = dp(8) }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        thumbs[i]?.let { setImageBitmap(it) }
                        setOnClickListener { download(v) }
                    }
                    binding.onlineRow.addView(iv)
                }
            }
        }.start()
    }

    private fun download(v: OnlineVideo) {
        if (busy) return
        busy = true
        showProgress(getString(R.string.downloading))
        Thread {
            val f = File(cacheDir, "online.mp4")
            val ok = VideoCatalog.download(v.videoUrl, f)
            runOnUiThread {
                busy = false
                hideProgress()
                if (!ok) {
                    Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val thumb = thumbnail { setDataSource(f.path) }
                if (thumb == null) { Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show(); return@runOnUiThread }
                pickedUri = Uri.fromFile(f)
                binding.preview.setImageBitmap(thumb)
                binding.btnSet.visibility = View.GONE
            }
        }.start()
    }

    private fun showProgress(text: String) {
        binding.progressText.text = text
        binding.progressText.visibility = View.VISIBLE
    }

    private fun hideProgress() { binding.progressText.visibility = View.GONE }

    override fun initAction() {
        binding.btnPick.setOnClickListener {
            @Suppress("DEPRECATION")
            startActivityForResult(WallpaperUi.pickIntent("video/*"), REQ_PICK)
        }
        binding.btnSave.setOnClickListener { save() }
        binding.btnSet.setOnClickListener { WallpaperUi.openWallpaperPicker(this, VideoWallpaperService::class.java) }
    }

    private fun thumbnail(source: MediaMetadataRetriever.() -> Unit): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.source()
            r.getFrameAtTime(0)
        } catch (e: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode != REQ_PICK || resultCode != RESULT_OK || uri == null) return
        val thumb = thumbnail { setDataSource(this@VideoActivity, uri) }
        if (thumb == null) {
            Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show()
            return
        }
        pickedUri = uri
        binding.preview.setImageBitmap(thumb)
        binding.btnSet.visibility = View.GONE
    }

    private fun save() {
        val uri = pickedUri ?: run {
            Toast.makeText(this, R.string.pick_video_first, Toast.LENGTH_SHORT).show(); return
        }
        if (busy) return
        busy = true
        // Online videos are already encoded light; only compress what the user picked from the device.
        // Fit/fill is applied when the wallpaper draws the video; only heavy device videos are re-encoded.
        store.videoFit = binding.modeFit.isChecked
        if (uri.scheme == "file" || VideoCompressor.isLight(this, uri)) {
            store(uri)
            return
        }
        val out = File(cacheDir, "compressed.mp4")
        showProgress(getString(R.string.optimizing, 0))
        VideoCompressor.compress(
            this, uri, out, false,
            onProgress = { showProgress(getString(R.string.optimizing, it)) },
            onDone = { ok ->
                if (ok) store(Uri.fromFile(out)) else {
                    busy = false
                    hideProgress()
                    Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    private fun store(uri: Uri) {
        showProgress(getString(R.string.saving))
        Thread {
            val ok = store.saveVideo(uri)
            runOnUiThread {
                busy = false
                hideProgress()
                if (ok) {
                    binding.btnSet.visibility = View.VISIBLE
                    pickedUri = null
                    Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private companion object { const val REQ_PICK = 1 }
}
