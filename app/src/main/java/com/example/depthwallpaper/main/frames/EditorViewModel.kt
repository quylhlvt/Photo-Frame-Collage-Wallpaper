package com.example.depthwallpaper.main.frames

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** State of the frame editor that the layout reacts to. */
class EditorViewModel : ViewModel() {
    /** Index of the selected photo, or -1. "Replace photo" shows while one is selected. */
    val selected = MutableLiveData(-1)

    /** "Set as wallpaper" appears after saving and hides again as soon as anything changes. */
    val canSet = MutableLiveData(false)
}
