package com.example.depthwallpaper.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** The languages offered on the language screen and the helper that applies the saved one. */
object LanguageManager {
    data class Language(val tag: String, val flag: String, val name: String)

    val languages = listOf(
        Language("en", "\uD83C\uDDEC\uD83C\uDDE7", "English"),
        Language("vi", "\uD83C\uDDFB\uD83C\uDDF3", "Ti\u1ebfng Vi\u1ec7t"),
        Language("hi", "\uD83C\uDDEE\uD83C\uDDF3", "\u0939\u093f\u0928\u094d\u0926\u0940"),
        Language("es", "\uD83C\uDDEA\uD83C\uDDF8", "Espa\u00f1ol"),
        Language("fr", "\uD83C\uDDEB\uD83C\uDDF7", "Fran\u00e7ais"),
        Language("pt", "\uD83C\uDDF5\uD83C\uDDF9", "Portugu\u00eas"),
        Language("in", "\uD83C\uDDEE\uD83C\uDDE9", "Bahasa Indonesia"),
        Language("de", "\uD83C\uDDE9\uD83C\uDDEA", "Deutsch"),
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
