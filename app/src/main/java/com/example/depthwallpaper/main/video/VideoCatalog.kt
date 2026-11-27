package com.example.depthwallpaper.main.video

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class OnlineVideo(val name: String, val thumbUrl: String, val videoUrl: String)

/**
 * Online video list. Expected JSON at [CATALOG_URL]:
 * `[{"name":"Rain","thumb":"https://.../rain.jpg","url":"https://.../rain.mp4"}, ...]`
 * Host the videos already encoded (H.264, 720p, 30 fps, no audio, <= 15 s).
 * All methods block: call them from a background thread.
 */
object VideoCatalog {
    // TODO: point this at your own list.
    const val CATALOG_URL = ""

    fun fetch(): List<OnlineVideo>? {
        if (CATALOG_URL.isBlank()) return null
        return runCatching {
            val arr = JSONArray(open(CATALOG_URL).inputStream.bufferedReader().use { it.readText() })
            List(arr.length()) {
                val o = arr.getJSONObject(it)
                OnlineVideo(o.optString("name"), o.optString("thumb"), o.getString("url"))
            }
        }.getOrNull()
    }

    fun thumb(url: String): Bitmap? = runCatching {
        open(url).inputStream.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()

    /** Downloads to [out] via a temp file; returns false on any failure or an empty file. */
    fun download(url: String, out: File): Boolean {
        val tmp = File(out.parentFile, out.name + ".part")
        return runCatching {
            val c = open(url)
            c.inputStream.use { i -> FileOutputStream(tmp).use { i.copyTo(it) } }
            val expected = c.contentLengthLong
            check(tmp.length() > 0 && (expected <= 0 || tmp.length() == expected))
            out.delete()
            tmp.renameTo(out)
        }.getOrDefault(false).also { if (!it) tmp.delete() }
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 20_000
    }
}
