package com.example.depthwallpaper.staticphoto

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.databinding.ActivityCollageEditorBinding
import com.example.depthwallpaper.core.ImageUtils
import com.example.depthwallpaper.core.WallpaperStore
import com.example.depthwallpaper.core.WallpaperUi

/**
 * Edits a static collage. Tap a frame to select it, then drag to move its photo and pinch to zoom.
 * "Choose photos" fills the selected frame, or - with nothing selected - adds photos (max 10), and the
 * suggested layouts follow the photo count. Save renders the collage as the static wallpaper image.
 */
class CollageEditorActivity : AbsBaseActivity<ActivityCollageEditorBinding>() {
    private lateinit var store: WallpaperStore

    private var photos = mutableListOf<Bitmap?>()
    private var adjusts = mutableListOf<CellAdjust>()
    private var layout: CollageLayout = Collages.layoutsFor(1)[0]
    private var theme: CollageTheme = Collages.themes[0]
    private var selected = -1
    private var pendingCell = -1

    override fun getLayoutId() = R.layout.activity_collage_editor

    override fun initView() {
        store = WallpaperStore(this)
        val held = PickedPhotos.take()
        val count = (held?.size ?: intent.getIntExtra(EXTRA_COUNT, 1)).coerceIn(1, Collages.MAX_PHOTOS)
        photos = held?.toMutableList<Bitmap?>() ?: MutableList(count) { null }
        adjusts = MutableList(count) { CellAdjust() }
        theme = Collages.theme(intent.getStringExtra(EXTRA_THEME)) ?: Collages.themes[0]
        layout = Collages.layout(intent.getStringExtra(EXTRA_LAYOUT))?.takeIf { it.count == count }
            ?: Collages.layoutsFor(count)[0]

        binding.preview.apply {
            this.layout = this@CollageEditorActivity.layout
            this.theme = this@CollageEditorActivity.theme
            photos = this@CollageEditorActivity.photos.toList()
            adjusts = this@CollageEditorActivity.adjusts
            showHint = true
            onCellTap = { onTap(it) }
            onAdjusted = { binding.btnSet.visibility = View.GONE }
        }
        for (t in Collages.themes) {
            binding.swatches.addView(View(this).apply {
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(t.top, t.bottom)).apply {
                    shape = GradientDrawable.OVAL
                    setStroke(dp(2), 0x66888888)
                }
                setOnClickListener { theme = t; binding.preview.theme = t; binding.btnSet.visibility = View.GONE }
            }, LinearLayout.LayoutParams(dp(32), dp(32)).also { it.marginEnd = dp(10) })
        }
        rebuildThumbs()
        refreshUi()
    }

    override fun initAction() {
        binding.btnPick.setOnClickListener { chooseClicked() }
        binding.btnRemove.setOnClickListener { removeSelected() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnSet.setOnClickListener { WallpaperUi.openWallpaperPicker(this) }
    }

    // ---- selection ----
    private fun onTap(cell: Int) {
        selected = if (cell < 0 || cell == selected) -1 else cell
        binding.preview.selected = selected
        refreshUi()
        // An empty frame opens the picker straight away.
        if (selected >= 0 && photos[selected] == null) pickImages(multiple = false, cell = selected)
    }

    private fun refreshUi() {
        val sel = selected >= 0
        binding.btnPick.text = getString(if (sel) R.string.replace_photo else R.string.add_photos)
        binding.btnRemove.visibility = if (sel && photos.size > 1) View.VISIBLE else View.GONE
        binding.hint.text = getString(if (sel) R.string.hint_selected else R.string.hint_tap)
    }

    private fun chooseClicked() {
        if (selected >= 0) pickImages(multiple = false, cell = selected) else pickImages(multiple = true, cell = -1)
    }

    private fun removeSelected() {
        val i = selected
        if (i < 0 || photos.size <= 1) return
        photos.removeAt(i)
        adjusts.removeAt(i)
        selected = -1
        binding.preview.selected = -1
        onPhotosChanged(countChanged = true)
    }

    // ---- layouts ----
    /** Suggest layouts for the new photo count, keeping the same style (e.g. "grid") when it exists. */
    private fun onPhotosChanged(countChanged: Boolean) {
        if (countChanged) {
            val key = layout.id.substringAfter('-')
            val options = Collages.layoutsFor(photos.size)
            layout = options.firstOrNull { it.id.substringAfter('-') == key } ?: options[0]
            binding.preview.layout = layout
            rebuildThumbs()
        }
        binding.preview.photos = photos.toList()
        binding.btnSet.visibility = View.GONE
        refreshUi()
    }

    private fun rebuildThumbs() {
        binding.thumbs.removeAllViews()
        binding.layoutLabel.text = getString(R.string.frames_for, photos.size)
        for (l in Collages.layoutsFor(photos.size)) {
            binding.thumbs.addView(LayoutThumbView(this, l).apply {
                chosen = l.id == layout.id
                setOnClickListener {
                    layout = l
                    binding.preview.layout = l
                    binding.btnSet.visibility = View.GONE
                    rebuildThumbs()
                }
            }, LinearLayout.LayoutParams(dp(40), dp(86)).also { it.marginEnd = dp(10) })
        }
    }

    // ---- picking ----
    private fun pickImages(multiple: Boolean, cell: Int) {
        pendingCell = cell
        @Suppress("DEPRECATION")
        startActivityForResult(WallpaperUi.pickIntent("image/*", multiple), REQ_PICK)
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
        val cell = pendingCell
        val chosen = if (cell >= 0) uris.take(1) else uris
        Thread {
            val side = ImageUtils.sideForCount(photos.size + chosen.size)
            val decoded = chosen.mapNotNull { ImageUtils.decodeUri(this, it, side) }
            runOnUiThread { applyPicked(decoded, cell) }
        }.start()
    }

    private fun applyPicked(decoded: List<Bitmap>, cell: Int) {
        if (decoded.isEmpty()) {
            Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show()
            return
        }
        if (cell >= 0 && cell < photos.size) {
            photos[cell] = decoded[0]
            adjusts[cell] = CellAdjust()
            onPhotosChanged(countChanged = false)
            return
        }
        // Add: fill empty frames first, then append new frames up to the limit.
        val before = photos.size
        var overflow = false
        for (bmp in decoded) {
            val empty = photos.indexOfFirst { it == null }
            when {
                empty >= 0 -> { photos[empty] = bmp; adjusts[empty] = CellAdjust() }
                photos.size < Collages.MAX_PHOTOS -> { photos.add(bmp); adjusts.add(CellAdjust()) }
                else -> overflow = true
            }
        }
        if (overflow) Toast.makeText(this, getString(R.string.max_photos, Collages.MAX_PHOTOS), Toast.LENGTH_SHORT).show()
        onPhotosChanged(countChanged = photos.size != before)
    }

    // ---- saving ----
    private fun save() {
        val first = photos.firstOrNull { it != null }
        if (first == null) {
            Toast.makeText(this, R.string.pick_first, Toast.LENGTH_SHORT).show()
            return
        }
        val filled = photos.map { it ?: first } // fill any empty frame
        val adj = adjusts.map { CellAdjust(it.zoom, it.cx, it.cy) }
        val l = layout
        val t = theme
        Thread {
            val bmp = Bitmap.createBitmap(OUT_W, OUT_H, Bitmap.Config.ARGB_8888)
            CollageRenderer().draw(Canvas(bmp), OUT_W.toFloat(), OUT_H.toFloat(), l, t, filled, adjusts = adj)
            store.saveStatic(bmp)
            bmp.recycle()
            runOnUiThread {
                for (i in photos.indices) if (photos[i] == null) photos[i] = filled[i]
                binding.preview.photos = photos.toList()
                binding.btnSet.visibility = View.VISIBLE
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    companion object {
        const val EXTRA_COUNT = "count"
        const val EXTRA_LAYOUT = "layout"
        const val EXTRA_THEME = "theme"
        private const val REQ_PICK = 1
        private const val OUT_W = 1080
        private const val OUT_H = 2340 // 9:19.5
    }
}
