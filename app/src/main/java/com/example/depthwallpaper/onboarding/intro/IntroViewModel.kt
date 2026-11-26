package com.example.depthwallpaper.onboarding.intro

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.depthwallpaper.R
import com.example.depthwallpaper.core.Event

class IntroViewModel : ViewModel() {
    val pages = IntroData.pages

    /** Current page; two-way bound to the ViewPager2 (`app:page="@={vm.page}"`). */
    val page = MutableLiveData(0)

    /** String resource of the main button: "Next" until the last page, then "Get started". */
    val buttonText: LiveData<Int> = page.map { if (it == pages.lastIndex) R.string.get_started else R.string.next }

    private val _finished = MutableLiveData<Event<Unit>>()
    val finished: LiveData<Event<Unit>> = _finished

    fun next() {
        val i = page.value ?: 0
        if (i < pages.lastIndex) page.value = i + 1 else _finished.value = Event(Unit)
    }
}
