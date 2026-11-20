package com.example.depthwallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/** Persists the user's chosen frame and one photo file per slot. */
class WallpaperStore(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("wallpaper", Context.MODE_PRIVATE)
    private val dir get() = File(ctx.filesDir, "slots").apply { mkdirs() }

    val version: Long get() = prefs.getLong("version", 0L)
    val frameId: String? get() = prefs.getString("frame", null)

    fun save(frame: FrameTemplate, photos: List<Bitmap?>) {
        dir.listFiles()?.forEach { it.delete() }
        photos.forEachIndexed { i, b ->
            if (b != null) FileOutputStream(File(dir, "s$i.jpg")).use { b.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        }
        prefs.edit().putString("frame", frame.id).putLong("version", System.currentTimeMillis()).commit()
    }

    fun load(): Pair<FrameTemplate, List<Bitmap?>>? {
        val frame = FrameTemplates.byId(frameId) ?: return null
        val photos = frame.slots.indices.map { i ->
            File(dir, "s$i.jpg").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
        }
        if (photos.all { it == null }) return null
        return frame to photos
    }
}
