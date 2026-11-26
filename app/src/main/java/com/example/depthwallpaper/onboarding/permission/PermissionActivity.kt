package com.example.depthwallpaper.onboarding.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.AppPrefs
import com.example.depthwallpaper.databinding.ActivityPermissionBinding
import com.example.depthwallpaper.home.MainActivity

/**
 * Asks for the one permission that applies to this Android version: notifications on 13+,
 * photo/storage access before that. Both are optional, so Continue always works.
 */
class PermissionActivity : AbsBaseActivity<ActivityPermissionBinding>() {
    private val isNotification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private val permission = if (isNotification) Manifest.permission.POST_NOTIFICATIONS
    else Manifest.permission.READ_EXTERNAL_STORAGE
    private val key get() = if (isNotification) "notification" else "storage"
    private val vm: PermissionViewModel by viewModels()
    private var requesting = false

    override fun getLayoutId() = R.layout.activity_permission

    override fun initView() {
        binding.vm = vm // texts, switch state and clicks go through the ViewModel
        vm.rowClicked.observe(this) { if (it.consume() != null) onRowClicked() }
        vm.continueClicked.observe(this) {
            if (it.consume() == null) return@observe
            AppPrefs.setPermissionDone(this)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        refresh()
    }

    override fun initAction() {}

    private fun granted() = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun onRowClicked() {
        when {
            granted() -> Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
            // Refused twice: the system won't show the popup any more, so open the app settings.
            AppPrefs.denyCount(this, key) >= 2 -> startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
            )
            else -> {
                requesting = true
                ActivityCompat.requestPermissions(this, arrayOf(permission), REQ)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        requesting = false
        if (requestCode == REQ && !granted()) AppPrefs.addDeny(this, key)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh() // back from the app settings
    }

    private fun refresh() {
        vm.granted.value = granted()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishAffinity()
    }

    private companion object { const val REQ = 1001 }
}
