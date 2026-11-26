package com.example.depthwallpaper.onboarding.intro

import android.content.Intent
import androidx.viewpager2.widget.ViewPager2
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.AppPrefs
import com.example.depthwallpaper.databinding.ActivityIntroBinding
import com.example.depthwallpaper.home.MainActivity
import com.example.depthwallpaper.onboarding.permission.PermissionActivity

/** Three swipeable pages; the last "Next" goes to the permission screen (or home if already done). */
class IntroActivity : AbsBaseActivity<ActivityIntroBinding>() {
    private val pages = IntroData.pages

    override fun getLayoutId() = R.layout.activity_intro

    override fun initView() {
        binding.viewPager.adapter = IntroAdapter(pages)
        updatePage(0)
    }

    override fun initAction() {
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updatePage(position)
        })
        binding.btnNext.setOnClickListener {
            val i = binding.viewPager.currentItem
            if (i < pages.size - 1) binding.viewPager.currentItem = i + 1 else finishIntro()
        }
    }

    private fun updatePage(position: Int) {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        dots.forEachIndexed { i, dot ->
            dot.setBackgroundResource(if (i == position) R.drawable.bg_intro_dot_active else R.drawable.bg_intro_dot_inactive)
        }
        binding.btnNext.setText(if (position == pages.size - 1) R.string.get_started else R.string.next)
    }

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
