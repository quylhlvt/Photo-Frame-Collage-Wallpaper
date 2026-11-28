package com.example.depthwallpaper.core.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

/** One photo from the device gallery. */
data class GalleryImage(val id: Long, val uri: Uri, val folderId: Long, val folderName: String)

/** A gallery folder ("bucket"). [id] is null for the "Recents" entry that holds every photo. */
data class GalleryFolder(val id: Long?, val name: String, val count: Int, val cover: Uri?)

/** Reads the device photos through MediaStore, newest first. Call from a background thread. */
object GalleryRepository {
    fun loadImages(context: Context): List<GalleryImage> {
        val out = ArrayList<GalleryImage>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                out.add(
                    GalleryImage(
                        id, ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                        c.getLong(bucketCol), c.getString(nameCol) ?: "",
                    ),
                )
            }
        }
        return out
    }

    /** "Recents" first, then the folders ordered by name. */
    fun folders(images: List<GalleryImage>, recentsName: String): List<GalleryFolder> {
        val result = mutableListOf(GalleryFolder(null, recentsName, images.size, images.firstOrNull()?.uri))
        images.groupBy { it.folderId }.values
            .map { GalleryFolder(it.first().folderId, it.first().folderName, it.size, it.first().uri) }
            .sortedBy { it.name.lowercase() }
            .forEach { result.add(it) }
        return result
    }
}
