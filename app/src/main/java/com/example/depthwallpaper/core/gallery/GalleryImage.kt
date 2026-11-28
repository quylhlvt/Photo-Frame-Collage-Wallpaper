package com.example.depthwallpaper.core.gallery

import android.net.Uri

/** One photo the user added; [id] is stable for the session (used for selection). */
data class GalleryImage(val id: Long, val uri: Uri)
