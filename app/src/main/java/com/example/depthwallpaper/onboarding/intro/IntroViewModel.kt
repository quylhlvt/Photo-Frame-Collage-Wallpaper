package com.example.depthwallpaper.onboarding.intro

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.depthwallpaper.core.Event

class IntroViewModel : ViewModel() {
    val pages = IntroData.pages

    /** Current page. */
    val page = MutableLiveData(0)

    private val _finished = MutableLiveData<Event<Unit>>()
    val finished: LiveData<Event<Unit>> = _finished

    fun next() {
        val i = page.value ?: 0
        if (i < pages.lastIndex) page.value = i + 1 else _finished.value = Event(Unit)
    }
}
