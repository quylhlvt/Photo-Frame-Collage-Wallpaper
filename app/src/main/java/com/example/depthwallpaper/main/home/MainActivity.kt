package com.example.depthwallpaper.main.home

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.GravityCompat
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.core.AppPrefs
import com.example.depthwallpaper.core.RateHelper
import com.example.depthwallpaper.core.SamplePhoto
import com.example.depthwallpaper.databinding.ActivityHomeBinding
import com.example.depthwallpaper.main.frames.EditorActivity
import com.example.depthwallpaper.main.frames.FrameListActivity
import com.example.depthwallpaper.main.frames.FramePreviewView
import com.example.depthwallpaper.main.frames.FrameTemplates
import com.example.depthwallpaper.language.LanguageActivity
import com.example.depthwallpaper.main.project.ProjectActivity
import com.example.depthwallpaper.main.staticphoto.StaticActivity
import com.example.depthwallpaper.main.video.VideoActivity

/**
 * Home: DIY wallpaper (static collage), DIY live wallpaper (frames), Dynamic (video), Project (saved
 * wallpapers), a "Hot viral" row of frames, and a side menu.
 */
class MainActivity : AbsBaseActivity<ActivityHomeBinding>(ActivityHomeBinding::inflate) {
    /** The drawer layout covers the whole window, so only the main content gets the status bar padding. */
    override fun insetsTarget(): View? = binding.content

    override fun initView() {
        fillHotRow()
    }

    override fun initAction() {
        binding.btnMenu.setOnClickListener { openDrawer() }
        binding.btnAvatar.setOnClickListener { openProject() }
        binding.cardStatic.setOnClickListener { openStatic() }
        binding.cardFrames.setOnClickListener { openFrames() }
        binding.cardVideo.setOnClickListener { openVideo() }
        binding.cardProject.setOnClickListener { openProject() }
        binding.btnViewAll.setOnClickListener { openFrames() }
        binding.itemLanguage.setOnClickListener { openLanguage() }
        binding.itemRate.setOnClickListener { rateUs() }
        binding.itemShare.setOnClickListener { shareApp() }
        binding.itemPrivacy.setOnClickListener { openPrivacy() }
    }

    /** A row of frame previews; tap one to open it in the editor. */
    private fun fillHotRow() {
        val sample = SamplePhoto.build(resources, 4)
        for (frame in FrameTemplates.all.take(8)) {
            val item = FrameLayout(this).apply {
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, EditorActivity::class.java).putExtra(FrameListActivity.EXTRA_FRAME, frame.id))
                }
            }
            item.addView(FramePreviewView(this).apply {
                template = frame
                photos = List(frame.slots.size) { sample }
            }, FrameLayout.LayoutParams((dp(230) * 9f / 19.5f).toInt(), dp(230)))
            binding.hotRow.addView(item, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).also { it.marginEnd = dp(12) })
        }
    }

    // ---- actions ----
    private fun openStatic() = startActivity(Intent(this, StaticActivity::class.java))
    private fun openFrames() = startActivity(Intent(this, FrameListActivity::class.java))
    private fun openVideo() = startActivity(Intent(this, VideoActivity::class.java))

    private fun openProject() = startActivity(Intent(this, ProjectActivity::class.java))

    private fun openDrawer() = binding.drawer.openDrawer(GravityCompat.START)

    private fun openLanguage() {
        binding.drawer.closeDrawers()
        startActivity(Intent(this, LanguageActivity::class.java))
    }

    private fun rateUs() {
        binding.drawer.closeDrawers()
        RateHelper.showRateDialog(this) {}
    }

    private fun shareApp() {
        binding.drawer.closeDrawers()
        val text = getString(R.string.share_text, getString(R.string.home_title)) +
            "\nhttps://play.google.com/store/apps/details?id=$packageName"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
    }

    private fun openPrivacy() {
        binding.drawer.closeDrawers()
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_url)))) }
    }

    /**
     * Back closes the menu if it is open. Otherwise it closes the whole app; on every second back
     * (until the user has rated) the rate dialog is shown first, like the reference apps.
     */
    override fun handleBackPressed(): Boolean {
        if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
            binding.drawer.closeDrawers()
            return true
        }
        val count = AppPrefs.backCount(this) + 1
        AppPrefs.setBackCount(this, count)
        if (!AppPrefs.isRated(this) && count % 2 == 0) {
            RateHelper.showRateDialog(this) { finishAffinity() }
        } else {
            finishAffinity()
        }
        return true
    }
}
