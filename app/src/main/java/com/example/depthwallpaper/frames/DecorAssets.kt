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

    /** True for the vector placeholders; your own PNG/WebP artwork keeps its colours. */
    fun isPlaceholder(name: String): Boolean {
        val d = ContextCompat.getDrawable(ctx, drawableId(name).takeIf { it != 0 } ?: return false) ?: return false
        return d !is android.graphics.drawable.BitmapDrawable
    }

    /** Height / width of the image. */
    fun aspect(name: String): Float {
        val d = ContextCompat.getDrawable(ctx, drawableId(name).takeIf { it != 0 } ?: return 1f) ?: return 1f
        val iw = d.intrinsicWidth
        val ih = d.intrinsicHeight
        return if (iw > 0 && ih > 0) ih.toFloat() / iw else 1f
    }

    private val windows = HashMap<String, FloatArray>()

    /**
     * Where the photo goes in a frame image: the transparent area that touches the centre, as
     * fractions [left, top, right, bottom] of the image. Falls back to a margin if there is none.
     */
    fun window(name: String): FloatArray = windows.getOrPut(name) {
        val bmp = bitmap(name, 160) ?: return@getOrPut floatArrayOf(0.06f, 0.06f, 0.94f, 0.80f)
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        fun clear(i: Int) = (px[i] ushr 24) < 24
        val start = (h / 2) * w + w / 2
        if (!clear(start)) return@getOrPut floatArrayOf(0.06f, 0.06f, 0.94f, 0.80f)
        val seen = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var head = 0
        var tail = 0
        queue[tail++] = start; seen[start] = true
        var minX = w; var minY = h; var maxX = 0; var maxY = 0
        while (head < tail) {
            val i = queue[head++]
            val x = i % w
            val y = i / w
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
            if (x > 0 && !seen[i - 1] && clear(i - 1)) { seen[i - 1] = true; queue[tail++] = i - 1 }
            if (x < w - 1 && !seen[i + 1] && clear(i + 1)) { seen[i + 1] = true; queue[tail++] = i + 1 }
            if (y > 0 && !seen[i - w] && clear(i - w)) { seen[i - w] = true; queue[tail++] = i - w }
            if (y < h - 1 && !seen[i + w] && clear(i + w)) { seen[i + w] = true; queue[tail++] = i + w }
        }
        floatArrayOf(minX / w.toFloat(), minY / h.toFloat(), (maxX + 1) / w.toFloat(), (maxY + 1) / h.toFloat())
    }

    private val masks = HashMap<String, Bitmap?>()

    /**
     * The photo's visible area in a frame image: opaque inside the window region, clear elsewhere,
     * same size as [bitmap] of the same width. Lets round or capsule windows cut the photo to shape.
     */
    fun windowMask(name: String, widthPx: Int): Bitmap? = masks.getOrPut("$name@$widthPx") {
        val fr = bitmap(name, widthPx) ?: return@getOrPut null
        val w = fr.width
        val h = fr.height
        val px = IntArray(w * h)
        fr.getPixels(px, 0, w, 0, 0, w, h)
        val start = (h / 2) * w + w / 2
        if ((px[start] ushr 24) >= 24) return@getOrPut null
        val out = IntArray(w * h)
        val queue = IntArray(w * h)
        var head = 0
        var tail = 0
        queue[tail++] = start; out[start] = -0x1000000
        while (head < tail) {
            val i = queue[head++]
            val x = i % w
            val y = i / w
            fun push(j: Int) { if (out[j] == 0 && (px[j] ushr 24) < 24) { out[j] = -0x1000000; queue[tail++] = j } }
            if (x > 0) push(i - 1)
            if (x < w - 1) push(i + 1)
            if (y > 0) push(i - w)
            if (y < h - 1) push(i + w)
        }
        Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun render(d: android.graphics.drawable.Drawable, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, w, h)
        d.draw(Canvas(bmp))
        return bmp
    }
}
