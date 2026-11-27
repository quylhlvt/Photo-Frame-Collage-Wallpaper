package com.example.depthwallpaper.main.staticphoto

import android.graphics.Bitmap

/** Hands decoded photos from the DIY screen's "+" button to the editor without re-reading URIs. */
object PickedPhotos {
    private var photos: List<Bitmap>? = null
    fun put(list: List<Bitmap>) { photos = list }
    fun take(): List<Bitmap>? = photos.also { photos = null }
}
