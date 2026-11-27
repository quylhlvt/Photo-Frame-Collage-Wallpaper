package com.example.depthwallpaper.onboarding.permission

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.depthwallpaper.R

/** State of the permission screen. The Activity does the actual asking; this holds what to show. */
class PermissionViewModel : ViewModel() {
    private val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val titleRes = if (modern) R.string.permission_notification else R.string.permission_storage
    val descRes = if (modern) R.string.permission_notification_desc else R.string.permission_storage_desc

    /** Whether the permission is allowed right now; bound to the switch. */
    val granted = MutableLiveData(false)
}
