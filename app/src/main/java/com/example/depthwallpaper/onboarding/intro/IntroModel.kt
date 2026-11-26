package com.example.depthwallpaper.onboarding.intro

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.depthwallpaper.R

data class IntroModel(
    val id: Int,
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val content: Int,
)

object IntroData {
    val pages = listOf(
        IntroModel(0, R.drawable.ic_intro_collage, R.string.intro_title_1, R.string.intro_content_1),
        IntroModel(1, R.drawable.ic_intro_frames, R.string.intro_title_2, R.string.intro_content_2),
        IntroModel(2, R.drawable.ic_intro_video, R.string.intro_title_3, R.string.intro_content_3),
    )
}
