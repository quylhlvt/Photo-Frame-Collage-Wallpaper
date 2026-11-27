package com.example.depthwallpaper.onboarding.intro

import android.content.Intent
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.AppPrefs
import com.example.depthwallpaper.databinding.ActivityIntroBinding
import com.example.depthwallpaper.main.home.MainActivity
import com.example.depthwallpaper.onboarding.permission.PermissionActivity

/** Three swipeable pages; the last "Next" goes to the permission screen (or home if already done). */
class IntroActivity : AbsBaseActivity<ActivityIntroBinding>(ActivityIntroBinding::inflate) {
    private val vm: IntroViewModel by viewModels()

    override fun initView() {
        binding.viewPager.adapter = IntroAdapter(vm.pages)
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        // The ViewModel holds the current page; the views follow it.
        vm.page.observe(this) { page ->
            if (binding.viewPager.currentItem != page) binding.viewPager.setCurrentItem(page, true)
            dots.forEachIndexed { i, dot ->
                dot.setBackgroundResource(if (i == page) R.drawable.bg_intro_dot_active else R.drawable.bg_intro_dot_inactive)
            }
        }
        vm.buttonText.observe(this) { binding.btnNext.setText(it) }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { vm.page.value = position }
        })
        binding.btnNext.setOnClickListener { vm.next() }
        vm.finished.observe(this) { if (it.consume() != null) finishIntro() }
    }

    override fun initAction() {}

    private fun finishIntro() {
        val next = if (AppPrefs.isPermissionDone(this)) MainActivity::class.java else PermissionActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }

    /** Back leaves the app, like the home screen. */
    override fun handleBackPressed(): Boolean {
        finishAffinity()
        return true
    }
}
