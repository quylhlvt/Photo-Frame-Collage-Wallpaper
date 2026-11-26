package com.example.depthwallpaper.language

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.depthwallpaper.core.Event
import com.example.depthwallpaper.core.LanguageManager

class LanguageViewModel : ViewModel() {
    val languages = LanguageManager.languages

    /** Tag of the highlighted language. */
    val selected = MutableLiveData("en")

    private val _confirmed = MutableLiveData<Event<String>>()
    val confirmed: LiveData<Event<String>> = _confirmed

    fun select(tag: String) { selected.value = tag }

    fun onDone() { _confirmed.value = Event(selected.value ?: "en") }
}
