package com.example.depthwallpaper.frames

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat

/**
 * Loads the sticker/background images by drawable NAME, so artwork can be swapped by dropping a
 * file with the same name into res/drawable-nodpi. Works for both vector XML and PNG/WebP.
 */
class DecorAssets(private val ctx: Context) {
    private val cache = HashMap<String, Bitmap?>()

    private fun drawableId(name: String) = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)

    /** The image [widthPx] wide, keeping its proportions. */
    fun bitmap(name: String, widthPx: Int): Bitmap? = cache.getOrPut("$name@$widthPx") {
        val d = ContextCompat.getDrawable(ctx, drawableId(name).takeIf { it != 0 } ?: return@getOrPut null) ?: return@getOrPut null
        val iw = d.intrinsicWidth.takeIf { it > 0 } ?: widthPx
        val ih = d.intrinsicHeight.takeIf { it > 0 } ?: widthPx
        val h = (widthPx.toFloat() * ih / iw).toInt().coerceAtLeast(1)
        render(d, widthPx, h)
    }

    /** The image stretched to exactly [w] x [h]. */
    fun bitmapExact(name: String, w: Int, h: Int): Bitmap? = cache.getOrPut("$name@${w}x$h") {
        val d = ContextCompat.getDrawable(ctx, drawableId(name).takeIf { it != 0 } ?: return@getOrPut null) ?: return@getOrPut null
        render(d, w, h)
    }

    private fun render(d: android.graphics.drawable.Drawable, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, w, h)
        d.draw(Canvas(bmp))
        return bmp
    }
}
