package com.example.depthwallpaper.onboarding.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.depthwallpaper.core.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Fills the loading bar over [DURATION_MS], then tells the screen to move on. */
class SplashViewModel : ViewModel() {
    /** 0..1000, bound to the ProgressBar in the layout. */
    val progress = MutableLiveData(0)

    private val _finished = MutableLiveData<Event<Unit>>()
    val finished: LiveData<Event<Unit>> = _finished

    init {
        viewModelScope.launch {
            val steps = 50
            repeat(steps) {
                delay(DURATION_MS / steps)
                progress.value = (it + 1) * 1000 / steps
            }
            _finished.value = Event(Unit)
        }
    }

    private companion object { const val DURATION_MS = 2_500L }
}
