package com.example.depthwallpaper.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import java.io.File
import java.util.UUID

/**
 * The list of photos added on the Choose photo screen. Picked photos are copied into the app's storage
 * (the system picker's access to them is only temporary) and listed in Room.
 */
class AddedPhotoRepository(context: Context) {
    private val app = context.applicationContext
    private val dao = AppDatabase.get(app).addedPhotos()
    private val dir get() = File(app.filesDir, "added").apply { mkdirs() }

    val photos: LiveData<List<AddedPhoto>> = dao.observeAll()

    /** Copies and saves the photos; call from a background thread. */
    fun add(uris: List<Uri>) {
        for (uri in uris) {
            runCatching {
                val file = File(dir, "${UUID.randomUUID()}.jpg")
                app.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                    ?: return@runCatching
                dao.insert(AddedPhoto(path = file.absolutePath))
            }
        }
    }

    /** Removes one photo from the list and deletes its copy; call from a background thread. */
    fun remove(id: Long) {
        dao.find(id)?.let { File(it.path).delete() }
        dao.delete(id)
    }
}
