package com.example.depthwallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Pick photos from the device into a frame, save, then set it as the wallpaper. */
class EditorActivity : Activity() {
    private lateinit var frame: FrameTemplate
    private lateinit var preview: FramePreviewView
    private lateinit var btnSet: Button
    private lateinit var store: WallpaperStore
    private val photos = mutableListOf<Bitmap?>()
    private var pendingSlot = -1

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        frame = FrameTemplates.byId(intent.getStringExtra(MainActivity.EXTRA_FRAME)) ?: run { finish(); return }
        store = WallpaperStore(this)
        repeat(frame.slots.size) { photos.add(null) }
        // Start from what is already saved for this frame, if any.
        if (store.frameId == frame.id) store.load()?.second?.forEachIndexed { i, b -> photos[i] = b }

        preview = FramePreviewView(this).apply {
            template = frame
            photos = this@EditorActivity.photos
            showHint = true
            onSlotTap = { pickImages(multiple = false, slot = it) }
        }
        val btnPick = Button(this).apply {
            text = getString(R.string.choose_photos)
            setOnClickListener { pickImages(multiple = true, slot = -1) }
        }
        val btnSave = Button(this).apply {
            text = getString(R.string.save)
            setOnClickListener { save() }
        }
        btnSet = Button(this).apply {
            text = getString(R.string.set_wallpaper)
            visibility = View.GONE
            setOnClickListener { openWallpaperPicker() }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            addView(TextView(context).apply {
                text = getString(R.string.editor_hint)
                textSize = 14f
                gravity = Gravity.CENTER
                alpha = 0.7f
            })
            addView(preview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0, 1f).also {
                it.topMargin = dp(8); it.bottomMargin = dp(8)
            })
            val lp = { LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
            addView(btnPick, lp())
            addView(btnSave, lp())
            addView(btnSet, lp())
        }
        setContentView(root)
    }

    private fun pickImages(multiple: Boolean, slot: Int) {
        pendingSlot = slot
        val intent = Intent(Intent.ACTION_GET_CONTENT)
            .setType("image/*")
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
        @Suppress("DEPRECATION")
        startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_photos)), REQ_PICK)
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
            val limit = if (slot >= 0) 1 else frame.slots.size
            val decoded = uris.take(limit).mapNotNull { ImageUtils.decodeUri(this, it) }
            runOnUiThread {
                if (decoded.isEmpty()) {
                    Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                if (slot >= 0) {
                    photos[slot] = decoded[0]
                } else {
                    // Fewer photos than slots: repeat them so every frame is filled.
                    for (i in photos.indices) photos[i] = decoded[i % decoded.size]
                }
                btnSet.visibility = View.GONE
                preview.photos = photos.toList()
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
            store.save(frame, filled)
            runOnUiThread {
                for (i in photos.indices) photos[i] = filled[i]
                preview.photos = photos.toList()
                btnSet.visibility = View.VISIBLE
                Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun openWallpaperPicker() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, DepthWallpaperService::class.java),
        )
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Some launchers don't support direct preview: fall back to the chooser.
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    private companion object {
        const val REQ_PICK = 1
    }
}
