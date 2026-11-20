package com.example.depthwallpaper

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** List of frames; tap one to open the editor. */
class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        list.addView(TextView(this).apply {
            text = getString(R.string.choose_frame)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(12))
        })

        val sample = SamplePhoto.build(resources, 4)
        for (frame in FrameTemplates.all) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
                isClickable = true
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, EditorActivity::class.java).putExtra(EXTRA_FRAME, frame.id))
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
            list.addView(row)
        }
        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(list)
        })
    }

    companion object {
        const val EXTRA_FRAME = "frame"
    }
}
