package com.example.depthwallpaper.onboarding.splash

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.AppPrefs
import com.example.depthwallpaper.databinding.ActivitySplashBinding
import com.example.depthwallpaper.home.MainActivity
import com.example.depthwallpaper.onboarding.intro.IntroActivity

/** First screen: logo and a short loading bar, then intro (first launch) or home. */
class SplashActivity : AbsBaseActivity<ActivitySplashBinding>() {
    private val handler = Handler(Looper.getMainLooper())
    private var goneOn = false
    private val go = Runnable { navigate() }

    override fun getLayoutId() = R.layout.activity_splash

    override fun initView() {
        binding.progress.max = 1000
        android.animation.ObjectAnimator.ofInt(binding.progress, "progress", 0, 1000).apply {
            duration = MIN_SPLASH_MS
            interpolator = LinearInterpolator()
            start()
        }
        handler.postDelayed(go, MIN_SPLASH_MS)
    }

    override fun initAction() {}

    private fun navigate() {
        if (goneOn || isFinishing || isDestroyed) return
        goneOn = true
        val next = if (AppPrefs.isIntroDone(this)) MainActivity::class.java else IntroActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Back is ignored while loading.
    }

    override fun onDestroy() {
        handler.removeCallbacks(go)
        super.onDestroy()
    }

    private companion object { const val MIN_SPLASH_MS = 2_500L }
}
