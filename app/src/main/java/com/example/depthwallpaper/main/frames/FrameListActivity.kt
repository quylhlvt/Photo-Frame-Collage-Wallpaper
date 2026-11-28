package com.example.depthwallpaper.main.frames

import android.content.ClipData
import android.content.Intent
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.setTitle
import com.example.depthwallpaper.core.setLeft
import com.example.depthwallpaper.core.SamplePhoto
import com.example.depthwallpaper.databinding.ActivityFrameListBinding
import com.example.depthwallpaper.main.staticphoto.picker.PhotoPickerActivity

/** Mode 2: list of frames; tap one to open the editor. */
class FrameListActivity : AbsBaseActivity<ActivityFrameListBinding>(ActivityFrameListBinding::inflate) {
    private var pendingFrame: String? = null

    override fun initView() {
        binding.actionBar.apply {
            setTitle(getString(R.string.choose_frame))
            setLeft { onBackPressedDispatcher.onBackPressed() }
        }
        val sample = SamplePhoto.build(resources, 4)
        for (frame in FrameTemplates.all) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
                isClickable = true
                // Tapping a frame first goes to "Choose photo", then to the editor with those photos in it.
                setOnClickListener {
                    pendingFrame = frame.id
                    @Suppress("DEPRECATION")
                    startActivityForResult(
                        Intent(this@FrameListActivity, PhotoPickerActivity::class.java)
                            .putExtra(PhotoPickerActivity.EXTRA_MAX, frame.slots.size),
                        REQ_PICK,
                    )
                }
            }
            row.addView(FramePreviewView(this).apply {
                template = frame
                photos = List(frame.slots.size) { sample }
            }, LinearLayout.LayoutParams((dp(220) * 9f / 19.5f).toInt(), dp(220)))
            row.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), 0, 0, 0)
                addView(TextView(context).apply { text = frame.name; textSize = 18f })
                addView(TextView(context).apply {
                    text = getString(R.string.photo_count, frame.slots.size)
                    textSize = 14f
                    alpha = 0.6f
                })
            })
            binding.list.addView(row)
        }
    }

    override fun initAction() {}

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val frameId = pendingFrame
        if (requestCode != REQ_PICK || resultCode != RESULT_OK || data == null || frameId == null) return
        // Hand the chosen photos on to the editor (read access travels with the ClipData).
        val clip = data.clipData ?: data.data?.let { ClipData.newRawUri("photo", it) } ?: return
        startActivity(
            Intent(this, EditorActivity::class.java).putExtra(EXTRA_FRAME, frameId).apply {
                clipData = clip
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }

    companion object {
        const val EXTRA_FRAME = "frame"
        private const val REQ_PICK = 3
    }
}
