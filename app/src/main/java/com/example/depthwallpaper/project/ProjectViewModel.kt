package com.example.depthwallpaper.project

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

/** What the Project screen shows: the list, and the multi-select mode (long press). */
class ProjectViewModel : ViewModel() {
    val projects = MutableLiveData<List<Project>>(emptyList())
    val isEmpty: LiveData<Boolean> = projects.map { it.isEmpty() }

    /** true while picking several projects (trash, Share and Download show). */
    val selecting = MutableLiveData(false)
    val selectedIds = MutableLiveData<Set<String>>(emptySet())

    fun startSelecting(id: String) {
        selecting.value = true
        selectedIds.value = setOf(id)
    }

    fun toggle(id: String) {
        val now = selectedIds.value.orEmpty()
        selectedIds.value = if (id in now) now - id else now + id
        // Nothing left selected: leave the mode.
        if (selectedIds.value.isNullOrEmpty()) selecting.value = false
    }

    fun stopSelecting() {
        selecting.value = false
        selectedIds.value = emptySet()
    }

    fun selectedProjects(): List<Project> {
        val ids = selectedIds.value.orEmpty()
        return projects.value.orEmpty().filter { it.id in ids }
    }
}
