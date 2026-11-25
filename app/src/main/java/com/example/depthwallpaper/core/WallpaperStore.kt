package com.example.depthwallpaper.core

import com.example.depthwallpaper.frames.*
import com.example.depthwallpaper.staticphoto.CellAdjust
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/** The three kinds of wallpaper the app can make. */
enum class WallpaperMode { FRAMES, STATIC, VIDEO }

data class SavedWallpaper(
    val frame: FrameTemplate,
    val photos: List<Bitmap?>,
    val bgColor: Int,
    val bgImage: Bitmap?,
    val adjusts: List<CellAdjust> = emptyList(),
)

/** Persists the active wallpaper mode plus the data for each mode. */
class WallpaperStore(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("wallpaper", Context.MODE_PRIVATE)
    private val dir get() = File(ctx.filesDir, "slots").apply { mkdirs() }
    private val bgFile get() = File(ctx.filesDir, "background.jpg")
    val staticFile get() = File(ctx.filesDir, "static.jpg")
    val videoFile get() = File(ctx.filesDir, "video.mp4")

    val version: Long get() = prefs.getLong("version", 0L)

    /** Video wallpaper placement: true keeps the whole picture visible, false fills and crops. */
    var videoFit: Boolean
        get() = prefs.getBoolean("videoFit", true)
        set(v) { prefs.edit().putBoolean("videoFit", v).apply() }
    val frameId: String? get() = prefs.getString("frame", null)
    val mode: WallpaperMode
        get() = runCatching { WallpaperMode.valueOf(prefs.getString("mode", null)!!) }
            .getOrDefault(WallpaperMode.FRAMES)

    /** Last image-based mode (STATIC or FRAMES); the image wallpaper ignores VIDEO. */
    val imageMode: WallpaperMode
        get() = runCatching { WallpaperMode.valueOf(prefs.getString("imageMode", null)!!) }
            .getOrDefault(WallpaperMode.FRAMES)

    private fun commit(mode: WallpaperMode, extra: android.content.SharedPreferences.Editor.() -> Unit = {}) {
        prefs.edit().putString("mode", mode.name).putLong("version", System.currentTimeMillis()).apply {
            if (mode != WallpaperMode.VIDEO) putString("imageMode", mode.name)
        }.apply(extra).commit()
    }

    // ---- Mode 2: frames ----
    fun save(frame: FrameTemplate, photos: List<Bitmap?>, bgColor: Int, bgImage: Bitmap?, adjusts: List<CellAdjust> = emptyList()) {
        dir.listFiles()?.forEach { it.delete() }
        bgFile.delete()
        if (bgImage != null) FileOutputStream(bgFile).use { bgImage.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        photos.forEachIndexed { i, b ->
            if (b != null) FileOutputStream(File(dir, "s$i.jpg")).use { b.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        }
        commit(WallpaperMode.FRAMES) {
            putString("frame", frame.id).putInt("bgColor", bgColor)
            for (i in frame.slots.indices) {
                val a = adjusts.getOrNull(i) ?: CellAdjust()
                putString("adj$i", "${a.zoom},${a.cx},${a.cy}")
            }
        }
    }

    fun load(): SavedWallpaper? {
        val frame = FrameTemplates.byId(frameId) ?: return null
        val photos = frame.slots.indices.map { i ->
            File(dir, "s$i.jpg").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
        }
        if (photos.all { it == null }) return null
        val bgImage = bgFile.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
        val adjusts = frame.slots.indices.map { i ->
            val p = prefs.getString("adj$i", null)?.split(",")?.mapNotNull { it.toFloatOrNull() }
            if (p != null && p.size == 3) CellAdjust(p[0], p[1], p[2]) else CellAdjust()
        }
        return SavedWallpaper(frame, photos, prefs.getInt("bgColor", FrameRenderer.BG_COLOR), bgImage, adjusts)
    }

    // ---- Mode 1: static photo ----
    fun saveStatic(bmp: Bitmap) {
        FileOutputStream(staticFile).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        commit(WallpaperMode.STATIC)
    }

    // ---- Mode 3: video ----
    /** Copies the picked video into app storage. Returns false if it could not be read. */
    fun saveVideo(uri: Uri): Boolean {
        val tmp = File(ctx.filesDir, "video.tmp")
        val ok = runCatching {
            ctx.contentResolver.openInputStream(uri)!!.use { input ->
                FileOutputStream(tmp).use { input.copyTo(it) }
            }
            videoFile.delete()
            tmp.renameTo(videoFile)
        }.getOrDefault(false)
        if (!ok) { tmp.delete(); return false }
        commit(WallpaperMode.VIDEO)
        return true
    }
}
