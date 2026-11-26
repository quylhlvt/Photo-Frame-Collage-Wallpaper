package com.example.depthwallpaper.staticphoto

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.ImageUtils
import com.example.depthwallpaper.core.SamplePhoto
import com.example.depthwallpaper.core.WallpaperUi
import com.example.depthwallpaper.databinding.ActivityStaticBinding

/** Mode 1 (DIY): ready-made collage templates. Tap one to edit it, or tap "+" to start from your own photos. */
class StaticActivity : AbsBaseActivity<ActivityStaticBinding>() {
    override fun getLayoutId() = R.layout.activity_static

    override fun initView() {
        binding.ui = this
        val sample = SamplePhoto.build(resources, 4)
        Collages.presets.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { p -> row.addView(templateCard(p, sample), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)) }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            binding.grid.addView(row)
        }
    }

    override fun initAction() {}

    /** The "+" button calls this from the layout. */
    fun addPhotos() {
        @Suppress("DEPRECATION")
        startActivityForResult(WallpaperUi.pickIntent("image/*", multiple = true), REQ_PICK)
    }

    private fun templateCard(p: CollagePreset, sample: Bitmap?): View {
        val layout = Collages.find(p.count, p.key)
        val theme = Collages.theme(p.themeId) ?: Collages.themes[0]
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(6), dp(6), dp(6), dp(10))
            isClickable = true
            setOnClickListener {
                startActivity(
                    Intent(this@StaticActivity, CollageEditorActivity::class.java)
                        .putExtra(CollageEditorActivity.EXTRA_COUNT, layout.count)
                        .putExtra(CollageEditorActivity.EXTRA_LAYOUT, layout.id)
                        .putExtra(CollageEditorActivity.EXTRA_THEME, theme.id),
                )
            }
        }
        box.addView(CollagePreviewView(this).apply {
            this.layout = layout; this.theme = theme
            photos = List(layout.count) { sample }
        }, LinearLayout.LayoutParams((dp(300) * 9f / 19.5f).toInt(), dp(300)))
        box.addView(TextView(this).apply {
            text = getString(R.string.photo_count, layout.count); textSize = 13f; alpha = 0.7f
            setPadding(0, dp(6), 0, 0)
        })
        return box
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PICK || resultCode != RESULT_OK || data == null) return
        val uris = mutableListOf<Uri>()
        data.clipData?.let { for (i in 0 until it.itemCount) uris.add(it.getItemAt(i).uri) }
        if (uris.isEmpty()) data.data?.let { uris.add(it) }
        if (uris.isEmpty()) return
        if (uris.size > Collages.MAX_PHOTOS) {
            Toast.makeText(this, getString(R.string.max_photos, Collages.MAX_PHOTOS), Toast.LENGTH_SHORT).show()
        }
        val chosen = uris.take(Collages.MAX_PHOTOS)
        Thread {
            val side = ImageUtils.sideForCount(chosen.size)
            val decoded = chosen.mapNotNull { ImageUtils.decodeUri(this, it, side) }
            runOnUiThread {
                if (decoded.isEmpty()) {
                    Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show()
                } else {
                    PickedPhotos.put(decoded)
                    startActivity(Intent(this, CollageEditorActivity::class.java).putExtra(CollageEditorActivity.EXTRA_COUNT, decoded.size))
                }
            }
        }.start()
    }

    private companion object { const val REQ_PICK = 1 }
}
