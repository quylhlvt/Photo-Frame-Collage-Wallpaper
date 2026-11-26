package com.example.depthwallpaper.core

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Draws behind a transparent status bar and hides the navigation bar (it comes back for a moment
 * on a swipe). Called by the base activity for every screen.
 *
 * @param isBlack true for light screens: the status bar icons are drawn dark.
 */
fun Activity.hideNavigation(isBlack: Boolean = false) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    @Suppress("DEPRECATION")
    window.statusBarColor = Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.navigationBarColor = Color.TRANSPARENT

    // Let the background fill the area around notches / water-drop cameras.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = isBlack
        isAppearanceLightNavigationBars = false
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        show(WindowInsetsCompat.Type.statusBars())
        hide(WindowInsetsCompat.Type.navigationBars())
    }
}

/** Tiny flags remembered between launches (onboarding progress). */
object AppPrefs {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("app", Context.MODE_PRIVATE)

    fun isIntroDone(ctx: Context) = prefs(ctx).getBoolean("introDone", false)
    fun setIntroDone(ctx: Context) = prefs(ctx).edit().putBoolean("introDone", true).apply()

    fun isPermissionDone(ctx: Context) = prefs(ctx).getBoolean("permissionDone", false)
    fun setPermissionDone(ctx: Context) = prefs(ctx).edit().putBoolean("permissionDone", true).apply()

    /** How many times a permission was refused; after two the system stops asking. */
    fun denyCount(ctx: Context, key: String) = prefs(ctx).getInt("deny_$key", 0)
    fun addDeny(ctx: Context, key: String) = prefs(ctx).edit().putInt("deny_$key", denyCount(ctx, key) + 1).apply()
}
