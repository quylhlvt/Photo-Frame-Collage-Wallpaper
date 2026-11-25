package com.example.depthwallpaper.core

import com.example.depthwallpaper.frames.*
import com.example.depthwallpaper.service.*
import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView

object WallpaperUi {
    /** Opens the system screen that sets this app's live wallpaper. */
    fun openWallpaperPicker(activity: Activity, service: Class<*> = DepthWallpaperService::class.java) {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(activity, service),
        )
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Some launchers don't support direct preview: fall back to the chooser.
            activity.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    fun pickIntent(mime: String, multiple: Boolean = false): Intent =
        Intent.createChooser(
            Intent(Intent.ACTION_GET_CONTENT).setType(mime)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple),
            null,
        )
}

/** Phone-shaped (9:19.5), rounded, centre-cropped image. */
class PhoneImageView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : ImageView(ctx, attrs) {
    init {
        scaleType = ScaleType.CENTER_CROP
        setBackgroundColor(FrameRenderer.BG_COLOR)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.08f)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension((h * 9f / 19.5f).toInt(), h)
    }
}
