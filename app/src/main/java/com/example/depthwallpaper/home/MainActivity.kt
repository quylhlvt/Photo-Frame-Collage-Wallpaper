package com.example.depthwallpaper.home

import android.content.Intent
import com.example.depthwallpaper.R
import com.example.depthwallpaper.base.AbsBaseActivity
import com.example.depthwallpaper.databinding.ActivityHomeBinding
import com.example.depthwallpaper.frames.FrameListActivity
import com.example.depthwallpaper.staticphoto.StaticActivity
import com.example.depthwallpaper.video.VideoActivity

/** Home: the three kinds of wallpaper. */
class MainActivity : AbsBaseActivity<ActivityHomeBinding>() {
    override fun getLayoutId() = R.layout.activity_home

    override fun initView() {
        binding.ui = this // the cards call the functions below straight from the layout
    }

    override fun initAction() {}

    fun openStatic() = startActivity(Intent(this, StaticActivity::class.java))
    fun openFrames() = startActivity(Intent(this, FrameListActivity::class.java))
    fun openVideo() = startActivity(Intent(this, VideoActivity::class.java))
}
