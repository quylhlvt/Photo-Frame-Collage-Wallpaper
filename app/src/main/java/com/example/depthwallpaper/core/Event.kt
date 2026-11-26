package com.example.depthwallpaper.core

/**
 * A one-shot message from a ViewModel to its screen (navigate, ask for a permission...), held in a
 * LiveData<Event<T>>. [consume] returns the value once and null afterwards, so it isn't replayed
 * after a rotation.
 */
class Event<out T>(private val value: T) {
    private var handled = false

    fun consume(): T? = if (handled) null else { handled = true; value }
}
