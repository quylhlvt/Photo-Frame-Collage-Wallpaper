package com.example.depthwallpaper.language

import android.content.Intent
import androidx.activity.viewModels
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.AppPrefs
import com.example.depthwallpaper.core.show
import com.example.depthwallpaper.core.setLeft
import com.example.depthwallpaper.core.setRight
import com.example.depthwallpaper.core.setTitle
import com.example.depthwallpaper.core.showLeft
import com.example.depthwallpaper.databinding.ActivityLanguageBinding
import com.example.depthwallpaper.onboarding.intro.IntroActivity

/**
 * Language choice. Shown once on the first launch (then it continues to the intro); after that it
 * can be opened again and simply closes when done.
 */
class LanguageActivity : AbsBaseActivity<ActivityLanguageBinding>(ActivityLanguageBinding::inflate) {
    private val vm: LanguageViewModel by viewModels()
    /** The back arrow is hidden on the very first launch. */
    private val firstTime get() = !AppPrefs.isLanguageDone(this)

    override fun initView() {
        binding.actionBar.apply {
            setTitle(getString(R.string.language))
            setLeft { onBackPressedDispatcher.onBackPressed() }
            showLeft(!firstTime)
            setRight(R.drawable.ic_check_blue) { vm.onDone() }
        }
        val adapter = LanguageAdapter(vm.languages) { vm.select(it.tag) }
        binding.list.adapter = adapter
        vm.selected.value = AppPrefs.language(this) ?: "en"
        vm.selected.observe(this) { adapter.setSelected(it) }
        vm.confirmed.observe(this) { event ->
            val tag = event.consume() ?: return@observe
            AppPrefs.setLanguage(this, tag)
            if (firstTime) {
                AppPrefs.setLanguageDone(this)
                startActivity(Intent(this, IntroActivity::class.java))
            }
            finish()
        }
    }

    override fun initAction() {}

    override fun handleBackPressed(): Boolean {
        if (firstTime) finishAffinity() else finish()
        return true
    }
}
