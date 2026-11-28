package com.example.depthwallpaper.main.staticphoto.picker

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.depthwallpaper.core.gallery.GalleryFolder
import com.example.depthwallpaper.core.gallery.GalleryImage
import com.example.depthwallpaper.core.gallery.GalleryRepository

/** State of the "Choose photo" screen: the gallery, the open folder and the chosen photos (in order). */
class PickerViewModel : ViewModel() {
    private var all: List<GalleryImage> = emptyList()

    private val _folders = MutableLiveData<List<GalleryFolder>>(emptyList())
    val folders: LiveData<List<GalleryFolder>> = _folders

    /** null = Recents (every photo). */
    val currentFolder = MutableLiveData<Long?>(null)
    val currentFolderName = MutableLiveData("")

    private val _visible = MutableLiveData<List<GalleryImage>>(emptyList())
    val visible: LiveData<List<GalleryImage>> = _visible

    private val _selected = MutableLiveData<List<GalleryImage>>(emptyList())
    val selected: LiveData<List<GalleryImage>> = _selected

    /** true while the folder list is shown instead of the photos. */
    val folderMode = MutableLiveData(false)

    var loaded = false
        private set

    fun load(context: Context, recentsName: String) {
        if (loaded) return
        loaded = true
        Thread {
            val images = GalleryRepository.loadImages(context.applicationContext)
            all = images
            _folders.postValue(GalleryRepository.folders(images, recentsName))
            currentFolderName.postValue(recentsName)
            _visible.postValue(images)
        }.start()
    }

    fun openFolder(folder: GalleryFolder) {
        currentFolder.value = folder.id
        currentFolderName.value = folder.name
        _visible.value = if (folder.id == null) all else all.filter { it.folderId == folder.id }
        folderMode.value = false
    }

    fun toggleFolderMode() { folderMode.value = folderMode.value != true }

    fun isSelected(image: GalleryImage) = _selected.value.orEmpty().any { it.id == image.id }

    /** Adds or removes a photo; returns false when adding would go over [max]. */
    fun toggle(image: GalleryImage, max: Int): Boolean {
        val list = _selected.value.orEmpty()
        if (list.any { it.id == image.id }) {
            _selected.value = list.filter { it.id != image.id }
            return true
        }
        if (list.size >= max) return false
        _selected.value = list + image
        return true
    }

    fun remove(image: GalleryImage) { _selected.value = _selected.value.orEmpty().filter { it.id != image.id } }
    fun clear() { _selected.value = emptyList() }
}
