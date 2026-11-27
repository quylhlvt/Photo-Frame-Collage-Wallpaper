package com.example.depthwallpaper.main.video

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** What the video screen shows; the layout reads these through `@{vm.xxx}`. */
class VideoViewModel : ViewModel() {
    /** Frame shown in the phone-shaped preview. */
    val preview = MutableLiveData<Bitmap?>()

    /** "Set as wallpaper" appears once the video is saved. */
    val canSet = MutableLiveData(false)

    /** Status line ("Downloading…", "Optimizing video… 40%"); null hides it. */
    val busyText = MutableLiveData<String?>(null)

    /** The online list is shown only when the catalog loaded. */
    val showOnline = MutableLiveData(false)

    /** Fit the whole video (true) or fill the screen and crop (false); two-way bound to the radio buttons. */
    val fit = MutableLiveData(true)
}
