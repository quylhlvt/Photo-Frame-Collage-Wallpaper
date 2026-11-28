package com.example.depthwallpaper.main.staticphoto.picker

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.depthwallpaper.core.gallery.GalleryImage

/** The photos the user has added, and which of them are chosen (in the order they were chosen). */
class PickerViewModel : ViewModel() {
    private var nextId = 1L

    private val _added = MutableLiveData<List<GalleryImage>>(emptyList())
    val added: LiveData<List<GalleryImage>> = _added

    private val _selected = MutableLiveData<List<GalleryImage>>(emptyList())
    val selected: LiveData<List<GalleryImage>> = _selected

    fun isSelected(image: GalleryImage) = _selected.value.orEmpty().any { it.id == image.id }

    /** Adds new photos (skipping ones already there) and chooses them while there is room under [max]. */
    fun add(uris: List<Uri>, max: Int) {
        val known = _added.value.orEmpty().map { it.uri }.toSet()
        val fresh = uris.filter { it !in known }.map { GalleryImage(nextId++, it) }
        if (fresh.isEmpty()) return
        _added.value = _added.value.orEmpty() + fresh
        val room = (max - _selected.value.orEmpty().size).coerceAtLeast(0)
        _selected.value = _selected.value.orEmpty() + fresh.take(room)
    }

    /** Chooses or un-chooses a photo; false when choosing would go over [max]. */
    fun toggle(image: GalleryImage, max: Int): Boolean {
        val list = _selected.value.orEmpty()
        if (list.any { it.id == image.id }) { _selected.value = list.filter { it.id != image.id }; return true }
        if (list.size >= max) return false
        _selected.value = list + image
        return true
    }

    /** Takes a photo out of the chosen ones (it stays in the grid). */
    fun unselect(image: GalleryImage) { _selected.value = _selected.value.orEmpty().filter { it.id != image.id } }
    fun clear() { _selected.value = emptyList() }
}
