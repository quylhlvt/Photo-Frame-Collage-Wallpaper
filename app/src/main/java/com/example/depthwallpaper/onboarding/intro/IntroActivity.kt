package com.example.depthwallpaper.onboarding.intro

import android.content.Intent
import androidx.activity.viewModels
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.AppPrefs
import com.example.depthwallpaper.databinding.ActivityIntroBinding
import com.example.depthwallpaper.home.MainActivity
import com.example.depthwallpaper.onboarding.permission.PermissionActivity

/** Three swipeable pages; the last "Next" goes to the permission screen (or home if already done). */
class IntroActivity : AbsBaseActivity<ActivityIntroBinding>() {
    private val vm: IntroViewModel by viewModels()

    override fun getLayoutId() = R.layout.activity_intro

    override fun initView() {
        binding.viewPager.adapter = IntroAdapter(vm.pages)
        binding.vm = vm // page, button text and dots come from the ViewModel through the layout
        vm.finished.observe(this) { if (it.consume() != null) finishIntro() }
    }

    override fun initAction() {}

    private fun finishIntro() {
        AppPrefs.setIntroDone(this)
        val next = if (AppPrefs.isPermissionDone(this)) MainActivity::class.java else PermissionActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishAffinity()
    }
}
