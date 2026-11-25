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
    fun openWallpaperPicker(
        activity: Activity,
        service: Class<*> = DepthWallpaperService::class.java,
        lockImage: (() -> Bitmap?)? = null,
    ) {
        if (lockImage != null && isXiaomi()) {
            AlertDialog.Builder(activity)
                .setTitle(R.string.lock_title)
                .setMessage(R.string.lock_message)
                .setPositiveButton(R.string.lock_set_image) { _, _ ->
                    Thread {
                        val ok = runCatching { setLockImage(activity, lockImage()) }.getOrDefault(false)
                        activity.runOnUiThread {
                            Toast.makeText(activity, if (ok) R.string.lock_done else R.string.lock_failed, Toast.LENGTH_SHORT).show()
                            launchPicker(activity, service)
                        }
                    }.start()
                }
                .setNegativeButton(R.string.continue_anyway) { _, _ -> launchPicker(activity, service) }
                .show()
            return
        }
        launchPicker(activity, service)
    }

    /**
     * Xiaomi (MIUI/HyperOS) draws the lock screen with its own wallpaper window, so a third-party
     * live wallpaper only shows on the home screen. A still image can be set for the lock screen.
     */
    private fun isXiaomi() = Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) ||
        Build.BRAND.equals("redmi", ignoreCase = true) || Build.BRAND.equals("poco", ignoreCase = true)

    private fun setLockImage(ctx: Context, bmp: Bitmap?): Boolean {
        bmp ?: return false
        WallpaperManager.getInstance(ctx).setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
        return true
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
