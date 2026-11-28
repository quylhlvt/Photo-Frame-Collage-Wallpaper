package com.example.depthwallpaper.main.picker

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.example.depthwallpaper.core.gallery.GalleryImage
import com.example.depthwallpaper.core.gallery.toGalleryImage
import com.example.depthwallpaper.data.AddedPhotoRepository

/** The added photos (kept in Room) and which of them are chosen right now, in the order they were chosen. */
class PickerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AddedPhotoRepository(app)

    val added: LiveData<List<GalleryImage>> = repo.photos.map { list -> list.map { it.toGalleryImage() } }

    private val _selected = MutableLiveData<List<GalleryImage>>(emptyList())
    val selected: LiveData<List<GalleryImage>> = _selected

    fun isSelected(image: GalleryImage) = _selected.value.orEmpty().any { it.id == image.id }

    /** Saves the picked photos into the list. */
    fun add(uris: List<Uri>) {
        if (uris.isEmpty()) return
        Thread { repo.add(uris) }.start()
    }

    /** Chooses or un-chooses a photo; false when choosing would go over [max]. */
    fun toggle(image: GalleryImage, max: Int): Boolean {
        val list = _selected.value.orEmpty()
        if (list.any { it.id == image.id }) { _selected.value = list.filter { it.id != image.id }; return true }
        if (list.size >= max) return false
        _selected.value = list + image
        return true
    }

    /** Takes a photo out of the chosen ones (it stays in the list). */
    fun unselect(image: GalleryImage) { _selected.value = _selected.value.orEmpty().filter { it.id != image.id } }
    fun clear() { _selected.value = emptyList() }

    /** Deletes a photo from the list for good. */
    fun delete(image: GalleryImage) {
        unselect(image)
        Thread { repo.remove(image.id) }.start()
    }
}
