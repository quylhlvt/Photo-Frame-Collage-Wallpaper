package com.example.depthwallpaper.onboarding.permission

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.depthwallpaper.R
import com.example.depthwallpaper.core.Event

/** State of the permission screen. The Activity does the actual asking; this holds what to show. */
class PermissionViewModel : ViewModel() {
    private val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Texts depend on the Android version: notifications on 13+, photo access before. */
    val titleRes = if (modern) R.string.permission_notification else R.string.permission_storage
    val descRes = if (modern) R.string.permission_notification_desc else R.string.permission_storage_desc

    /** Whether the permission is allowed right now; bound to the switch. */
    val granted = MutableLiveData(false)

    private val _rowClicked = MutableLiveData<Event<Unit>>()
    val rowClicked: LiveData<Event<Unit>> = _rowClicked

    private val _continueClicked = MutableLiveData<Event<Unit>>()
    val continueClicked: LiveData<Event<Unit>> = _continueClicked

    fun onRowClick() { _rowClicked.value = Event(Unit) }
    fun onContinue() { _continueClicked.value = Event(Unit) }
}
