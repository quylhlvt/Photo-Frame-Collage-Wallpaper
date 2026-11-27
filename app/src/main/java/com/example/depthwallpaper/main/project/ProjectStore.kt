package com.example.depthwallpaper.main.project

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.depthwallpaper.core.WallpaperStore
import com.example.depthwallpaper.main.frames.FrameRenderer
import com.example.depthwallpaper.main.frames.FrameTemplate
import com.example.depthwallpaper.main.frames.FrameTemplates
import com.example.depthwallpaper.main.staticphoto.CellAdjust
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

enum class ProjectType { FRAMES, STATIC, VIDEO }

/** One saved wallpaper. Everything needed to set it again lives in [dir]. */
data class Project(val id: String, val type: ProjectType, val created: Long, val dir: File) {
    val thumb: File get() = File(dir, "thumb.jpg")
    val thumbPath: String get() = thumb.path
    val video: File get() = File(dir, "video.mp4")
}

/**
 * The list of saved wallpapers ("Projects"). Every Save in the editors adds one, so it can be set
 * again, downloaded, shared or deleted later. Each project is a folder in `filesDir/projects`.
 */
class ProjectStore(private val ctx: Context) {
    private val root get() = File(ctx.filesDir, "projects").apply { mkdirs() }

    fun list(): List<Project> = (root.listFiles() ?: emptyArray())
        .mapNotNull { dir -> readMeta(dir)?.let { m ->
            runCatching { Project(dir.name, ProjectType.valueOf(m.getString("type")), m.getLong("created"), dir) }.getOrNull()
        } }
        .filter { it.thumb.exists() }
        .sortedByDescending { it.created }

    private fun readMeta(dir: File): JSONObject? =
        runCatching { JSONObject(File(dir, "meta.json").readText()) }.getOrNull()

    private fun newDir(): File {
        val id = System.currentTimeMillis().toString()
        return File(root, id).apply { mkdirs() }
    }

    private fun writeThumb(dir: File, bmp: Bitmap) {
        val thumb = Bitmap.createScaledBitmap(bmp, THUMB_W, THUMB_H, true)
        FileOutputStream(File(dir, "thumb.jpg")).use { thumb.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (thumb !== bmp) thumb.recycle()
    }

    private fun writeMeta(dir: File, type: ProjectType, extra: JSONObject.() -> Unit = {}) {
        val m = JSONObject().put("type", type.name).put("created", System.currentTimeMillis())
        m.extra()
        File(dir, "meta.json").writeText(m.toString())
    }

    private fun jpeg(file: File, bmp: Bitmap, quality: Int) =
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, quality, it) }

    // ---------------- add ----------------

    fun addFrames(frame: FrameTemplate, photos: List<Bitmap?>, bgColor: Int, bgImage: Bitmap?, adjusts: List<CellAdjust>) {
        val dir = newDir()
        photos.forEachIndexed { i, b -> if (b != null) jpeg(File(dir, "s$i.jpg"), b, 92) }
        if (bgImage != null) jpeg(File(dir, "bg.jpg"), bgImage, 90)
        writeMeta(dir, ProjectType.FRAMES) {
            put("frame", frame.id)
            put("bgColor", bgColor)
            put("adj", JSONArray().also { a -> adjusts.forEach { a.put(JSONArray().put(it.zoom.toDouble()).put(it.cx.toDouble()).put(it.cy.toDouble())) } })
        }
        val preview = renderFrames(frame, photos, bgColor, bgImage, adjusts, THUMB_W, THUMB_H)
        writeThumb(dir, preview)
        preview.recycle()
    }

    fun addStatic(bmp: Bitmap) {
        val dir = newDir()
        jpeg(File(dir, "static.jpg"), bmp, 92)
        writeMeta(dir, ProjectType.STATIC)
        writeThumb(dir, bmp)
    }

    fun addVideo(video: File, fit: Boolean) {
        val dir = newDir()
        video.copyTo(File(dir, "video.mp4"), overwrite = true)
        val frame = frameAt(File(dir, "video.mp4")) ?: return run { dir.deleteRecursively() }
        writeMeta(dir, ProjectType.VIDEO) { put("fit", fit) }
        writeThumb(dir, frame)
        frame.recycle()
    }

    private fun frameAt(file: File): Bitmap? {
        val r = MediaMetadataRetriever()
        return try { r.setDataSource(file.path); r.getFrameAtTime(0) } catch (e: Exception) { null } finally { runCatching { r.release() } }
    }

    // ---------------- use ----------------

    fun delete(p: Project) { p.dir.deleteRecursively() }

    /** Makes [p] the active wallpaper again. Returns false if its files can't be read. */
    fun apply(p: Project): Boolean {
        val store = WallpaperStore(ctx)
        return runCatching {
            val meta = readMeta(p.dir)!!
            when (p.type) {
                ProjectType.STATIC -> store.saveStatic(BitmapFactory.decodeFile(File(p.dir, "static.jpg").path)!!)
                ProjectType.VIDEO -> {
                    store.videoFit = meta.optBoolean("fit", true)
                    check(store.saveVideo(Uri.fromFile(p.video)))
                }
                ProjectType.FRAMES -> {
                    val frame = FrameTemplates.byId(meta.getString("frame"))!!
                    store.save(frame, loadPhotos(p, frame), meta.getInt("bgColor"), loadBg(p), loadAdjusts(meta, frame))
                }
            }
            true
        }.getOrDefault(false)
    }

    private fun loadPhotos(p: Project, frame: FrameTemplate) =
        frame.slots.indices.map { i -> File(p.dir, "s$i.jpg").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) } }

    private fun loadBg(p: Project) = File(p.dir, "bg.jpg").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    private fun loadAdjusts(meta: JSONObject, frame: FrameTemplate): List<CellAdjust> {
        val a = meta.optJSONArray("adj")
        return frame.slots.indices.map { i ->
            val e = a?.optJSONArray(i)
            if (e != null) CellAdjust(e.getDouble(0).toFloat(), e.getDouble(1).toFloat(), e.getDouble(2).toFloat()) else CellAdjust()
        }
    }

    private fun renderFrames(
        frame: FrameTemplate, photos: List<Bitmap?>, bgColor: Int, bg: Bitmap?, adj: List<CellAdjust>, w: Int, h: Int,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        FrameRenderer(ctx).draw(Canvas(bmp), w.toFloat(), h.toFloat(), frame, photos, 1f, bg, false, bgColor, adj)
        return bmp
    }

    /** The full-size picture of an image project (null for videos). */
    fun renderImage(p: Project): Bitmap? = runCatching {
        when (p.type) {
            ProjectType.STATIC -> BitmapFactory.decodeFile(File(p.dir, "static.jpg").path)
            ProjectType.FRAMES -> {
                val meta = readMeta(p.dir)!!
                val frame = FrameTemplates.byId(meta.getString("frame"))!!
                renderFrames(frame, loadPhotos(p, frame), meta.getInt("bgColor"), loadBg(p), loadAdjusts(meta, frame), OUT_W, OUT_H)
            }
            ProjectType.VIDEO -> null
        }
    }.getOrNull()

    // ---------------- export ----------------

    /**
     * Saves the project to the phone's gallery (Pictures/ or Movies/ "Collage Wallpaper").
     * Returns false on any failure. Before Android 10 the caller needs the storage permission.
     */
    fun saveToGallery(p: Project): Boolean = runCatching {
        val name = "Collage_${p.id}"
        if (p.type == ProjectType.VIDEO) {
            export(p.video, name + ".mp4", "video/mp4", isVideo = true)
        } else {
            val bmp = renderImage(p) ?: return@runCatching false
            val tmp = File(ctx.cacheDir, "export.jpg")
            jpeg(tmp, bmp, 95)
            bmp.recycle()
            export(tmp, name + ".jpg", "image/jpeg", isVideo = false)
        }
    }.getOrDefault(false)

    private fun export(src: File, name: String, mime: String, isVideo: Boolean): Boolean {
        val folder = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/Collage Wallpaper")
        }
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: return false
        resolver.openOutputStream(uri)?.use { out -> src.inputStream().use { it.copyTo(out) } } ?: return false
        return true
    }

    /** A content:// link another app can read, for sharing. */
    fun shareUri(p: Project): Uri? = runCatching {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val file = if (p.type == ProjectType.VIDEO) {
            File(dir, "share_${p.id}.mp4").also { p.video.copyTo(it, overwrite = true) }
        } else {
            val bmp = renderImage(p) ?: return@runCatching null
            File(dir, "share_${p.id}.jpg").also { jpeg(it, bmp, 95); bmp.recycle() }
        }
        FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
    }.getOrNull()

    private companion object {
        const val THUMB_W = 360
        const val THUMB_H = 780
        const val OUT_W = 1080
        const val OUT_H = 2340
    }
}
