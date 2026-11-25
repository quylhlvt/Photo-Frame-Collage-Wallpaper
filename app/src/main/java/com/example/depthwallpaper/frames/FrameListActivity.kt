package com.example.depthwallpaper.frames

import android.content.Intent
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.SamplePhoto
import com.example.depthwallpaper.databinding.ActivityFrameListBinding

/** Mode 2: list of frames; tap one to open the editor. */
class FrameListActivity : AbsBaseActivity<ActivityFrameListBinding>() {
    override fun getLayoutId() = R.layout.activity_frame_list

    override fun initView() {
        val sample = SamplePhoto.build(resources, 4)
        for (frame in FrameTemplates.all) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
                isClickable = true
                setOnClickListener {
                    startActivity(Intent(this@FrameListActivity, EditorActivity::class.java).putExtra(EXTRA_FRAME, frame.id))
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

    companion object {
        const val EXTRA_FRAME = "frame"
    }
}
