package com.example.depthwallpaper.core

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import com.example.depthwallpaper.R
import java.util.Locale

/** The languages offered on the language screen and the helper that applies the saved one. */
object LanguageManager {
    data class Language(val tag: String, @DrawableRes val flagRes: Int = 0, val name: String)

    val languages = listOf(
        Language("en",flagRes= R.drawable.ic_en, "English"),
        Language("es", flagRes= R.drawable.ic_sp, "Espa\u00f1ol"),
        Language("fr", flagRes= R.drawable.ic_fr, "Fran\u00e7ais"),
        Language("hi",  flagRes= R.drawable.ic_hi, "\u0939\u093f\u0928\u094d\u0926\u0940"),
        Language("pt", flagRes= R.drawable.ic_pt, "Portugu\u00eas"),
        Language("de", flagRes= R.drawable.ic_de, "Deutsch"),
        Language("in", flagRes= R.drawable.ic_in, "Bahasa Indonesia"),
    )

    /** Wraps [base] so its resources use the saved language. Called from the base activity. */
    fun wrap(base: Context): Context {
        val tag = AppPrefs.language(base) ?: return base
        val locale = Locale(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
