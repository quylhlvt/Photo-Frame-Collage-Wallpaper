package com.example.depthwallpaper.onboarding.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.AppPrefs
import com.example.depthwallpaper.core.setTitle
import com.example.depthwallpaper.databinding.ActivityPermissionBinding
import com.example.depthwallpaper.main.home.MainActivity

/**
 * Asks for the one permission that applies to this Android version: notifications on 13+,
 * photo/storage access before that. Both are optional, so Continue always works.
 */
class PermissionActivity : AbsBaseActivity<ActivityPermissionBinding>(ActivityPermissionBinding::inflate) {
    private val isNotification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private val permission = if (isNotification) Manifest.permission.POST_NOTIFICATIONS
    else Manifest.permission.READ_EXTERNAL_STORAGE
    private val key get() = if (isNotification) "notification" else "storage"
    private val vm: PermissionViewModel by viewModels()
    private var requesting = false

    override fun initView() {
        binding.actionBar.apply {

            setTitle(getString(R.string.permission),false)}
        binding.permName.setText(vm.titleRes)
        binding.permTitle.text = SpannableStringBuilder().apply {
            append(getString(R.string.permission_allow_prefix)).append(' ')
            val start = length
            append(getString(R.string.app_name))
            setSpan(ForegroundColorSpan(ContextCompat.getColor(this@PermissionActivity, R.color.app_color)), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(' ').append(getString(if (isNotification) R.string.permission_request_suffix_13 else R.string.permission_request_suffix))
        }
        vm.granted.observe(this) { binding.permSwitch.setImageResource(if (it) R.drawable.switch_on else R.drawable.switch_off) }
        binding.rowPermission.setOnClickListener { onRowClicked() }
        binding.btnContinue.setOnClickListener {
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
            AppPrefs.denyCount(this, key) >= 2 -> showSettingsDialog()
            else -> {
                requesting = true
                ActivityCompat.requestPermissions(this, arrayOf(permission), REQ)
            }
        }
    }

    /** Same as app 201: explain, then send the user to the app settings. */
    private fun showSettingsDialog() {
        val message = if (isNotification) R.string.permission_settings_notification else R.string.permission_settings_storage
        AlertDialog.Builder(this)
            .setTitle(R.string.permission)
            .setMessage(message)
            .setPositiveButton(R.string.permission_go_settings) { d, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                d.dismiss()
            }
            .setNegativeButton(R.string.permission_cancel) { d, _ -> d.dismiss() }
            .setCancelable(false)
            .show()
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

    override fun handleBackPressed(): Boolean {
        finishAffinity()
        return true
    }

    private companion object { const val REQ = 1001 }
}
