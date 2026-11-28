package com.example.depthwallpaper.onboarding.splash

import android.content.Intent
import androidx.activity.viewModels
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.AppPrefs
import com.example.depthwallpaper.databinding.ActivitySplashBinding
import com.example.depthwallpaper.language.LanguageActivity
import com.example.depthwallpaper.onboarding.intro.IntroActivity

/** First screen: logo and a short loading bar, then language (first launch only) and the intro. */
class SplashActivity : AbsBaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate) {
    private val vm: SplashViewModel by viewModels()

    /** The logo is centred, so no inset padding: the layout stays still while the bars hide on start. */
    override val applyInsets = false

    override fun initView() {
//        vm.progress.observe(this) { binding.progress.progress = it }
        vm.finished.observe(this) { event ->
            if (event.consume() != null) navigate()
        }
    }

    override fun initAction() {}

    private fun navigate() {
        // First launch: choose a language. Then the intro every launch; it leads to the permission screen
        // (until it is done) and finally home.
        val next = if (!AppPrefs.isLanguageDone(this)) LanguageActivity::class.java else IntroActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }

    /** Back is ignored while loading. */
    override fun handleBackPressed() = true
}
