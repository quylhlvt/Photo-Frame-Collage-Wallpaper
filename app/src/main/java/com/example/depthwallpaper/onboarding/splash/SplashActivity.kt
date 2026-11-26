package com.example.depthwallpaper.onboarding.splash

import android.content.Intent
import androidx.activity.viewModels
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.AppPrefs
import com.example.depthwallpaper.databinding.ActivitySplashBinding
import com.example.depthwallpaper.home.MainActivity
import com.example.depthwallpaper.onboarding.intro.IntroActivity

/** First screen: logo and a short loading bar, then intro (first launch) or home. */
class SplashActivity : AbsBaseActivity<ActivitySplashBinding>() {
    private val vm: SplashViewModel by viewModels()

    override fun getLayoutId() = R.layout.activity_splash

    override fun initView() {
        binding.vm = vm
        vm.finished.observe(this) { event ->
            if (event.consume() != null) navigate()
        }
    }

    override fun initAction() {}

    private fun navigate() {
        val next = if (AppPrefs.isIntroDone(this)) MainActivity::class.java else IntroActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Back is ignored while loading.
    }
}
