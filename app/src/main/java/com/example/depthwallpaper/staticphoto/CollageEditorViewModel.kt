package com.example.depthwallpaper.staticphoto

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** State of the collage editor that the layout reacts to (button texts, hints, visibility). */
class CollageEditorViewModel : ViewModel() {
    /** Selected frame, or -1. Changes the main button, the hint and shows "Remove". */
    val selected = MutableLiveData(-1)

    /** Number of photos/frames in the collage (1..10). */
    val photoCount = MutableLiveData(1)

    /** "Set as wallpaper" appears after saving and hides again as soon as anything changes. */
    val canSet = MutableLiveData(false)
}
