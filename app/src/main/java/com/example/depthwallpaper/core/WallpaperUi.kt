package com.example.depthwallpaper.core

import com.example.depthwallpaper.frames.*
import com.example.depthwallpaper.service.*
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import com.example.depthwallpaper.R
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
        // The system screen only offers the targets the phone supports (e.g. home only on some OEMs).
        launchPicker(activity, service)
    }

    /**
     * vivo/OPPO draw the lock screen themselves and can't show a live wallpaper there; the
     * wallpaper's effect is played on the home screen right after unlocking instead.
     */
    fun lockIgnoresLive(): Boolean {
        val oem = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()
        return listOf("vivo", "iqoo", "oppo", "realme").any { it in oem }
    }

    /**
     * If one of our live wallpapers is active but the lock screen still has its own still image
     * (set earlier by the user or the system), drop that image so the lock screen follows the
     * live wallpaper. Skipped on OEMs whose lock screen ignores live wallpapers anyway.
     */
    fun syncLockScreen(ctx: Context) {
        if (lockIgnoresLive() || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        runCatching {
            val wm = WallpaperManager.getInstance(ctx)
            if (wm.wallpaperInfo?.packageName == ctx.packageName && wm.getWallpaperId(WallpaperManager.FLAG_LOCK) > 0) {
                wm.clear(WallpaperManager.FLAG_LOCK)
            }
        }
    }

    private fun launchPicker(activity: Activity, service: Class<*>) {
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
