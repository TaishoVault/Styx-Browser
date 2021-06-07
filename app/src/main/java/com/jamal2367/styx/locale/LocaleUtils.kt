package com.jamal2367.styx.locale

import android.content.Context
import android.content.res.Resources
import java.util.*

/**
 * Utility class to manage overriding system language with user selected one.
 */
object LocaleUtils {

    /**
     * Provide the locale we should currently apply.
     */
    @Suppress("DEPRECATION")
    @JvmStatic
    fun requestedLocale(aUserLocale: String): Locale {
        return if (aUserLocale.isEmpty()) {
            // Provide the current system locale then
            Resources.getSystem().configuration.locale
        } else parseLocaleCode(aUserLocale)
    }

    /**
     * Provides a locale object from the given sting.
     * @param localeCode
     * @return
     */
    fun parseLocaleCode(localeCode: String): Locale {
        var index: Int
        if (localeCode.indexOf('-').also { index = it } != -1 ||
            localeCode.indexOf('_').also { index = it } != -1) {
            val langCode = localeCode.substring(0, index)
            val countryCode = localeCode.substring(index + 1)
            return Locale(langCode, countryCode)
        }
        return Locale(localeCode)
    }

    /**
     * This is public to allow for an activity to force the
     * current locale to be applied if necessary (e.g., when
     * a new activity launches).
     */
    @Suppress("DEPRECATION")
    private fun updateConfiguration(context: Context, locale: Locale) {
        val res = context.resources
        val config = res.configuration
        if (config.locale === locale) {
            // Already in the correct locale
            return
        }

        // We should use setLocale, but it's unexpectedly missing
        // on real devices.
        config.locale = locale
        config.setLayoutDirection(locale)
        res.updateConfiguration(config, null)
    }

    /**
     * Change locale of the JVM instance and given activity.
     *
     * @param context Activity context, do not pass application context as it seems it breaks everything
     * @param locale
     * @return The Java locale string: e.g., "en_US".
     */
    @JvmStatic
    fun updateLocale(context: Context, locale: Locale): String {
        // Fast path.
        Locale.setDefault(locale)
        // Update resources.
        updateConfiguration(context, locale)
        return locale.toString()
    }

    /**
     * Returns a list of supported locale codes
     */
    fun getPackagedLocaleTags(): Collection<String> {
        return Collections.unmodifiableList(listOf("ar", "cs", "de", "el", "en-US", "es", "fr", "hu", "it", "iw", "ja", "ko", "lt", "nl", "no", "pl", "pt", "pt-BR", "ro", "ru", "sr", "sv", "th", "tr", "uk", "vi", "zh-CN", "zh-TW"))
    }
}