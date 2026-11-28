package com.example.depthwallpaper.main.staticphoto

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.ImageUtils
import com.example.depthwallpaper.core.setLeft
import com.example.depthwallpaper.core.setTitle
import com.example.depthwallpaper.databinding.ActivityChooseFrameBinding

/**
 * "Choose Frame": after the photos were chosen with "+", pick one of the collage layouts made for that
 * number of photos, then Next opens the editor.
 */
class ChooseFrameActivity : AbsBaseActivity<ActivityChooseFrameBinding>(ActivityChooseFrameBinding::inflate) {
    private var photos: List<Bitmap> = emptyList()
    private lateinit var layouts: List<CollageLayout>
    private var selected = 0
    private val theme get() = Collages.themes[0]

    override fun initView() {
        val clip = intent.clipData
        val uris = clip?.let { c -> (0 until c.itemCount).map { c.getItemAt(it).uri } }.orEmpty().take(Collages.MAX_PHOTOS)
        if (uris.isEmpty()) { finish(); return }
        layouts = Collages.layoutsFor(uris.size)
        binding.actionBar.apply {
            setTitle(getString(R.string.picker_choose_frame))
            setLeft { onBackPressedDispatcher.onBackPressed() }
        }
        binding.preview.theme = theme
        binding.preview.layout = layouts[0]
        decode(uris)
        buildStrip()
    }

    override fun initAction() {
        binding.btnNext.setOnClickListener {
            if (photos.isEmpty()) return@setOnClickListener
            PickedPhotos.put(photos)
            startActivity(
                Intent(this, CollageEditorActivity::class.java)
                    .putExtra(CollageEditorActivity.EXTRA_COUNT, photos.size)
                    .putExtra(CollageEditorActivity.EXTRA_LAYOUT, layouts[selected].id)
                    .putExtra(CollageEditorActivity.EXTRA_THEME, theme.id),
            )
            finish()
        }
    }

    private fun decode(uris: List<Uri>) {
        Thread {
            val side = ImageUtils.sideForCount(uris.size)
            val decoded = uris.mapNotNull { ImageUtils.decodeUri(this, it, side) }
            runOnUiThread {
                if (decoded.isEmpty()) {
                    Toast.makeText(this, R.string.load_failed, Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }
                photos = decoded
                binding.preview.photos = decoded
                (0 until binding.layoutStrip.childCount).forEach { i ->
                    ((binding.layoutStrip.getChildAt(i) as FrameLayout).getChildAt(0) as CollagePreviewView).photos = decoded
                }
            }
        }.start()
    }

    /** One small preview per layout; the picked one gets an orange frame. */
    private fun buildStrip() {
        binding.layoutStrip.removeAllViews()
        layouts.forEachIndexed { i, l ->
            val w = dp(56)
            val h = dp(64)
            val cell = FrameLayout(this).apply {
                setPadding(dp(2), dp(2), dp(2), dp(2))
                setOnClickListener { select(i) }
            }
            cell.addView(CollagePreviewView(this).apply {
                layout = l; theme = this@ChooseFrameActivity.theme; photos = this@ChooseFrameActivity.photos
            }, FrameLayout.LayoutParams(w, h))
            binding.layoutStrip.addView(cell, android.widget.LinearLayout.LayoutParams(w + dp(4), h + dp(4)).apply { marginEnd = dp(10) })
        }
        select(0)
    }

    private fun select(i: Int) {
        selected = i
        binding.preview.layout = layouts[i]
        (0 until binding.layoutStrip.childCount).forEach { k ->
            binding.layoutStrip.getChildAt(k).setBackgroundResource(if (k == i) R.drawable.bg_layout_selected else 0)
        }
    }
}
