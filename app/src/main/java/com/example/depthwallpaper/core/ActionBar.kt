package com.example.depthwallpaper.core

import android.view.View
import androidx.annotation.DrawableRes
import com.example.depthwallpaper.R
import com.example.depthwallpaper.databinding.LayoutActionbarBinding

/*
 * Helpers for the shared top bar (layout_actionbar.xml). Typical use:
 *
 *   binding.actionBar.apply {
 *       setTitle(getString(R.string.language))
 *       setLeft { onBackPressedDispatcher.onBackPressed() }
 *       setRight(R.drawable.ic_check_blue) { save() }
 *   }
 */

/** Sets the title, centred (default) or aligned to the start. */
fun LayoutActionbarBinding.setTitle(text: CharSequence, center: Boolean = true) {
    val tv = if (center) tvCenter else tvStart
    (if (center) tvStart else tvCenter).visibility = View.GONE
    tv.text = text
    tv.visibility = View.VISIBLE
    tv.isSelected = true // lets long titles scroll
}

/** Shows the left button (a back arrow unless [icon] is given). */
fun LayoutActionbarBinding.setLeft(@DrawableRes icon: Int = R.drawable.ic_back, onClick: () -> Unit) {
    btnActionBarLeft.setImageResource(icon)
    btnActionBarLeft.visibility = View.VISIBLE
    btnActionBarLeft.setOnClickListener { onClick() }
}

/** Shows the right-most button. */
fun LayoutActionbarBinding.setRight(@DrawableRes icon: Int, onClick: () -> Unit) {
    btnActionBarRight.setImageResource(icon)
    btnActionBarRight.visibility = View.VISIBLE
    btnActionBarRight.setOnClickListener { onClick() }
}

/** Shows the button next to the right-most one. */
fun LayoutActionbarBinding.setRightSecond(@DrawableRes icon: Int, onClick: () -> Unit) {
    btnActionBarRight1.setImageResource(icon)
    btnActionBarRight1.visibility = View.VISIBLE
    btnActionBarRight1.setOnClickListener { onClick() }
}

/** Hides or shows (keeping its space) the right-most button, e.g. only while selecting. */
fun LayoutActionbarBinding.showRight(show: Boolean) {
    btnActionBarRight.visibility = if (show) View.VISIBLE else View.INVISIBLE
}

/** Hides or shows (keeping its space) the left button. */
fun LayoutActionbarBinding.showLeft(show: Boolean) {
    btnActionBarLeft.visibility = if (show) View.VISIBLE else View.INVISIBLE
}
