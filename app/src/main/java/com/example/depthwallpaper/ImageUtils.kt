package com.example.depthwallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.content.res.Resources
import android.media.ExifInterface
import android.net.Uri
import kotlin.math.max
import kotlin.math.min

object ImageUtils {
    /** Decode an image picked by the user, downsampled to ~[maxSide] and rotated per EXIF. */
    fun decodeUri(ctx: Context, uri: Uri, maxSide: Int = 1024): Bitmap? = runCatching {
        val r = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        r.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bmp = r.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val orientation = runCatching {
            r.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            else -> return bmp
        }
        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }.getOrNull()
}

/** The bundled sample image (background + cat), cropped around the cat. */
object SamplePhoto {
    fun build(res: Resources, sample: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val b = BitmapFactory.decodeResource(res, R.drawable.wp_bg, opts) ?: return null
        val s = BitmapFactory.decodeResource(res, R.drawable.wp_subject, opts) ?: return null
        val full = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(full)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        c.drawBitmap(b, null, Rect(0, 0, b.width, b.height), p)
        c.drawBitmap(s, null, Rect(0, 0, b.width, b.height), p)
        b.recycle(); s.recycle()
        val x0 = (full.width * 0.20f).toInt()
        val x1 = (full.width * 0.80f).toInt()
        val y0 = (full.height * 0.27f).toInt()
        val y1 = min(full.height, (full.height * 0.57f).toInt())
        return Bitmap.createBitmap(full, x0, y0, x1 - x0, y1 - y0)
    }
}
