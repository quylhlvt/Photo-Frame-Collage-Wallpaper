package com.example.depthwallpaper.frames

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.ImageUtils
import com.example.depthwallpaper.core.WallpaperStore
import com.example.depthwallpaper.core.WallpaperUi
import com.example.depthwallpaper.databinding.ActivityFrameEditorBinding

/** Pick photos from the device into a frame, save, then set it as the wallpaper. */
class EditorActivity : AbsBaseActivity<ActivityFrameEditorBinding>() {
    private lateinit var frame: FrameTemplate
    private lateinit var store: WallpaperStore
    private val photos = mutableListOf<Bitmap?>()
    private var pendingSlot = -1
    private var bgColor = FrameRenderer.BG_COLOR
    private var bgImage: Bitmap? = null

    override fun getLayoutId() = R.layout.activity_frame_editor

    override fun initView() {
        frame = FrameTemplates.byId(intent.getStringExtra(FrameListActivity.EXTRA_FRAME)) ?: run { finish(); return }
        store = WallpaperStore(this)
        repeat(frame.slots.size) { photos.add(null) }
        // Start from what is already saved for this frame, if any.
        if (store.frameId == frame.id) store.load()?.let { saved ->
            saved.photos.forEachIndexed { i, b -> photos[i] = b }
            bgColor = saved.bgColor
            bgImage = saved.bgImage
        }

        binding.preview.apply {
            template = frame
            photos = this@EditorActivity.photos
            showHint = true
            bgColor = this@EditorActivity.bgColor
            bgImage = this@EditorActivity.bgImage
            onSlotTap = { pickImages(multiple = false, slot = it) }
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
        binding.btnSave.setOnClickListener { save() }
        binding.btnSet.setOnClickListener { WallpaperUi.openWallpaperPicker(this) {
            // Still image of the final layout for lock screens that can't run the live wallpaper.
            store.load()?.let { saved ->
                val bmp = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
                FrameRenderer().draw(Canvas(bmp), 1080f, 2400f, saved.frame, saved.photos, 1f, saved.bgImage, bgColor = saved.bgColor)
                bmp
            }
        } }
    }

    private fun setBackground(color: Int, image: Bitmap?) {
        bgColor = color
        bgImage = image
        binding.preview.bgColor = color
        binding.preview.bgImage = image
        binding.btnSet.visibility = View.GONE
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
        val slot = pendingSlot
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
                } else {
                    // Fewer photos than slots: repeat them so every frame is filled.
                    for (i in photos.indices) photos[i] = decoded[i % decoded.size]
                }
                binding.btnSet.visibility = View.GONE
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
            store.save(frame, filled, bgColor, bgImage)
            runOnUiThread {
                for (i in photos.indices) photos[i] = filled[i]
                binding.preview.photos = photos.toList()
                binding.btnSet.visibility = View.VISIBLE
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
