package com.example.depthwallpaper.core

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
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

/** Shows the view, or hides it completely (GONE). */
fun View.show(show: Boolean) { visibility = if (show) View.VISIBLE else View.GONE }

/** Tiny flags remembered between launches (onboarding progress). */
object AppPrefs {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("app", Context.MODE_PRIVATE)

    fun isIntroDone(ctx: Context) = prefs(ctx).getBoolean("introDone", false)
    fun setIntroDone(ctx: Context) = prefs(ctx).edit().putBoolean("introDone", true).apply()

    /** The language screen is shown once, on the very first launch. */
    fun isLanguageDone(ctx: Context) = prefs(ctx).getBoolean("languageDone", false)
    fun setLanguageDone(ctx: Context) = prefs(ctx).edit().putBoolean("languageDone", true).apply()

    /** Language chosen by the user (BCP-47 tag such as "vi"); null follows the phone. */
    fun language(ctx: Context): String? = prefs(ctx).getString("language", null)
    fun setLanguage(ctx: Context, tag: String) = prefs(ctx).edit().putString("language", tag).apply()

    /** Back presses on the home screen; the rate dialog comes up on every second one. */
    fun backCount(ctx: Context) = prefs(ctx).getInt("backCount", 0)
    fun setBackCount(ctx: Context, n: Int) = prefs(ctx).edit().putInt("backCount", n).apply()

    /** True once the user has answered the rate dialog (or rated), so it isn't shown again. */
    fun isRated(ctx: Context) = prefs(ctx).getBoolean("rated", false)
    fun setRated(ctx: Context) = prefs(ctx).edit().putBoolean("rated", true).apply()

    fun isPermissionDone(ctx: Context) = prefs(ctx).getBoolean("permissionDone", false)
    fun setPermissionDone(ctx: Context) = prefs(ctx).edit().putBoolean("permissionDone", true).apply()

    /** How many times a permission was refused; after two the system stops asking. */
    fun denyCount(ctx: Context, key: String) = prefs(ctx).getInt("deny_$key", 0)
    fun addDeny(ctx: Context, key: String) = prefs(ctx).edit().putInt("deny_$key", denyCount(ctx, key) + 1).apply()
}
