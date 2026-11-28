package com.example.depthwallpaper.core.gallery

import android.net.Uri
import com.example.depthwallpaper.data.AddedPhoto
import java.io.File

/** One photo of the added list; [id] is its Room id. */
data class GalleryImage(val id: Long, val uri: Uri)

fun AddedPhoto.toGalleryImage() = GalleryImage(id, Uri.fromFile(File(path)))
