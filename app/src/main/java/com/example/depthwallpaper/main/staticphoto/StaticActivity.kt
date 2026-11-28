package com.example.depthwallpaper.main.staticphoto

import android.content.Intent
import android.graphics.Bitmap
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.ImageUtils
import com.example.depthwallpaper.core.SamplePhoto
import com.example.depthwallpaper.core.setLeft
import com.example.depthwallpaper.data.AddedPhotoRepository
import com.example.depthwallpaper.core.setRight
import com.example.depthwallpaper.core.setTitle
import com.example.depthwallpaper.databinding.ActivityStaticBinding
import com.example.depthwallpaper.main.staticphoto.picker.PhotoPickerActivity

/** Mode 1 (DIY): ready-made collage templates. Tap one to edit it, or tap "+" to start from your own photos. */
class StaticActivity : AbsBaseActivity<ActivityStaticBinding>(ActivityStaticBinding::inflate) {
    /** The template that was tapped (null: started from "+"), kept while "Choose photo" is open. */
    private var pendingLayout: String? = null
    private var pendingTheme: String? = null
    private var pendingCount = 0

    override fun initView() {
        // The previews show the photos the user added (the newest ones); the sample photo when there are none.
        val sample = SamplePhoto.build(resources, 4)
        Thread {
            val mine = AddedPhotoRepository(this).previewBitmaps(400)
            runOnUiThread { buildGrid(mine.ifEmpty { listOfNotNull(sample) }) }
        }.start()
    }

    private fun buildGrid(pool: List<Bitmap>) {
        Collages.presets.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { p -> row.addView(templateCard(p, pool), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)) }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            binding.grid.addView(row)
        }
    }

    override fun initAction() {
        binding.actionBar.apply {
            setTitle(getString(R.string.diy))
            setLeft { onBackPressedDispatcher.onBackPressed() }
            setRight(R.drawable.ic_add_pink) {
                pendingLayout = null
                pendingTheme = null
                pendingCount = 0
                openPicker(Collages.MAX_PHOTOS)
            }
        }
    }

    private fun openPicker(max: Int) {
        @Suppress("DEPRECATION")
        startActivityForResult(
            Intent(this, PhotoPickerActivity::class.java).putExtra(PhotoPickerActivity.EXTRA_MAX, max),
            REQ_PICK,
        )
    }

    private fun templateCard(p: CollagePreset, pool: List<Bitmap>): View {
        val layout = Collages.find(p.count, p.key)
        val theme = Collages.theme(p.themeId) ?: Collages.themes[0]
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(6), dp(6), dp(6), dp(10))
            isClickable = true
            // A template first goes to "Choose photo", then opens in the editor with those photos.
            setOnClickListener {
                pendingLayout = layout.id
                pendingTheme = theme.id
                pendingCount = layout.count
                openPicker(layout.count)
            }
        }
        box.addView(CollagePreviewView(this).apply {
            this.layout = layout; this.theme = theme
            photos = List(layout.count) { pool.getOrNull(it % pool.size.coerceAtLeast(1)) }
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
        val uris = PhotoPickerActivity.urisFrom(data)
        if (uris.isEmpty()) return
        val layoutId = pendingLayout
        if (layoutId == null) {
            // Started from "+": the number of photos decides which frames are offered.
            startActivity(Intent(this, ChooseFrameActivity::class.java).putStringArrayListExtra(PhotoPickerActivity.EXTRA_PATHS, data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_PATHS)))
            return
        }
        // A template was tapped: open it with the chosen photos.
        val chosen = uris.take(pendingCount.coerceAtLeast(1))
        Thread {
            val side = ImageUtils.sideForCount(pendingCount.coerceAtLeast(chosen.size))
            val decoded = chosen.mapNotNull { ImageUtils.decodeUri(this, it, side) }
            runOnUiThread {
                if (decoded.isEmpty()) {
                    Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                // Fewer photos than the template has frames: repeat them so every frame is filled.
                val photos = if (pendingCount > decoded.size) List(pendingCount) { decoded[it % decoded.size] } else decoded
                PickedPhotos.put(photos)
                startActivity(
                    Intent(this, CollageEditorActivity::class.java)
                        .putExtra(CollageEditorActivity.EXTRA_COUNT, photos.size)
                        .putExtra(CollageEditorActivity.EXTRA_LAYOUT, layoutId)
                        .putExtra(CollageEditorActivity.EXTRA_THEME, pendingTheme),
                )
            }
        }.start()
    }

    private companion object { const val REQ_PICK = 1 }
}
