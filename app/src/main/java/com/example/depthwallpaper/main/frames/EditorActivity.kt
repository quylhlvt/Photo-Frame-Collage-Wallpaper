package com.example.depthwallpaper.main.frames

import android.content.Intent
import android.graphics.Bitmap
import com.example.depthwallpaper.main.project.ProjectStore
import androidx.activity.viewModels
import com.example.depthwallpaper.core.show
import com.example.depthwallpaper.main.staticphoto.CellAdjust
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.setTitle
import com.example.depthwallpaper.core.setLeft
import com.example.depthwallpaper.core.ImageUtils
import com.example.depthwallpaper.core.WallpaperStore
import com.example.depthwallpaper.core.WallpaperUi
import com.example.depthwallpaper.databinding.ActivityFrameEditorBinding
import com.example.depthwallpaper.main.picker.PhotoPickerActivity

/** Pick photos from the device into a frame, save, then set it as the wallpaper. */
class EditorActivity : AbsBaseActivity<ActivityFrameEditorBinding>(ActivityFrameEditorBinding::inflate) {
    private lateinit var frame: FrameTemplate
    private lateinit var store: WallpaperStore
    private val photos = mutableListOf<Bitmap?>()
    private val adjusts = mutableListOf<CellAdjust>()
    private val vm: EditorViewModel by viewModels()
    private var selected: Int
        get() = vm.selected.value ?: -1
        set(v) { vm.selected.value = v }
    private var pendingSlot = -1
    private var bgColor = FrameRenderer.BG_COLOR
    private var bgImage: Bitmap? = null

    override fun initView() {
        binding.actionBar.apply {
            setTitle(getString(R.string.app_name))
            setLeft { onBackPressedDispatcher.onBackPressed() }
        }
        frame = FrameTemplates.byId(intent.getStringExtra(FrameListActivity.EXTRA_FRAME)) ?: run { finish(); return }
        store = WallpaperStore(this)
        // The buttons that depend on the state follow the ViewModel.
        vm.selected.observe(this) { binding.btnReplace.show(it >= 0) }
        vm.canSet.observe(this) { binding.btnSet.show(it) }
        // The photo lists are rebuilt on every start (e.g. after a rotation), so reset what depends on them.
        vm.selected.value = -1
        vm.canSet.value = false
        bgColor = frame.defaultBg
        repeat(frame.slots.size) { photos.add(null); adjusts.add(CellAdjust()) }
        // Start from what is already saved for this frame, if any.
        if (store.frameId == frame.id) store.load()?.let { saved ->
            saved.photos.forEachIndexed { i, b -> photos[i] = b }
            saved.adjusts.forEachIndexed { i, a -> if (i < adjusts.size) adjusts[i] = a }
            bgColor = saved.bgColor
            bgImage = saved.bgImage
        }

        // Photos chosen on the "Choose photo" screen before the editor opened.
        PhotoPickerActivity.urisFrom(intent).takeIf { it.isNotEmpty() }?.let { applyUris(it, -1) }

        binding.preview.apply {
            template = frame
            photos = this@EditorActivity.photos
            showHint = true
            bgColor = this@EditorActivity.bgColor
            bgImage = this@EditorActivity.bgImage
            adjusts = this@EditorActivity.adjusts
            onSlotTap = { onSlotTapped(it) }
            onAdjusted = { vm.canSet.value = false }
        }
        for (c in BG_COLORS) {
            binding.swatches.addView(View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(c)
                    setStroke(dp(2), 0x66888888)
                }
                setOnClickListener { setBackground(c, null) }
            }, LinearLayout.LayoutParams(dp(36), dp(36)).also { it.marginEnd = dp(10) })
        }
        binding.swatches.addView(Button(this).apply {
            text = getString(R.string.bg_photo)
            setOnClickListener { pickImages(multiple = false, slot = SLOT_BG) }
        })
    }

    override fun initAction() {
        binding.btnPick.setOnClickListener { pickImages(multiple = true, slot = -1) }
        binding.btnReplace.setOnClickListener { if (selected >= 0) pickImages(multiple = false, slot = selected) }
        binding.btnSave.setOnClickListener { save() }
        binding.btnSet.setOnClickListener { WallpaperUi.openWallpaperPicker(this) }
    }

    /** Empty card: choose a photo for it. Filled card: select it so it can be moved and zoomed. */
    private fun onSlotTapped(i: Int) {
        if (i < 0) { select(-1); return }
        if (photos[i] == null) { pickImages(multiple = false, slot = i); return }
        select(if (selected == i) -1 else i)
    }

    private fun select(i: Int) {
        selected = i
        binding.preview.selected = i
    }

    private fun setBackground(color: Int, image: Bitmap?) {
        bgColor = color
        bgImage = image
        binding.preview.bgColor = color
        binding.preview.bgImage = image
        vm.canSet.value = false
    }

    private fun pickImages(multiple: Boolean, slot: Int) {
        pendingSlot = slot
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
        applyUris(uris, pendingSlot)
    }

    /** Decodes the chosen photos and puts them in [slot] (-1: fill every frame, repeating if fewer). */
    private fun applyUris(uris: List<Uri>, slot: Int) {
        Thread {
            val limit = if (slot != -1) 1 else frame.slots.size
            val decoded = uris.take(limit).mapNotNull { ImageUtils.decodeUri(this, it) }
            runOnUiThread {
                if (decoded.isEmpty()) {
                    Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                if (slot == SLOT_BG) {
                    setBackground(bgColor, decoded[0])
                } else if (slot >= 0) {
                    photos[slot] = decoded[0]
                    adjusts[slot] = CellAdjust()
                } else {
                    // Fewer photos than slots: repeat them so every frame is filled.
                    for (i in photos.indices) { photos[i] = decoded[i % decoded.size]; adjusts[i] = CellAdjust() }
                }
                vm.canSet.value = false
                binding.preview.photos = photos.toList()
            }
        }.start()
    }

    private fun save() {
        val first = photos.firstOrNull { it != null }
        if (first == null) {
            Toast.makeText(this, R.string.pick_first, Toast.LENGTH_SHORT).show()
            return
        }
        val filled = photos.map { it ?: first } // fill any empty slot
        Thread {
            val adj = adjusts.map { CellAdjust(it.zoom, it.cx, it.cy) }
            store.save(frame, filled, bgColor, bgImage, adj)
            // Every Save also adds an entry to the Project list.
            runCatching { ProjectStore(this).addFrames(frame, filled, bgColor, bgImage, adj) }
            runOnUiThread {
                for (i in photos.indices) photos[i] = filled[i]
                binding.preview.photos = photos.toList()
                vm.canSet.value = true
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private companion object {
        const val REQ_PICK = 1
        const val SLOT_BG = -2 // pendingSlot value meaning "picking the background photo"
        val BG_COLORS = intArrayOf(
            0xFF3A3A3A.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFF8C8D8.toInt(),
            0xFFFFE0A3.toInt(), 0xFFBFE3C0.toInt(), 0xFFB8D8F8.toInt(), 0xFFD3C0F0.toInt(),
        )
    }
}
