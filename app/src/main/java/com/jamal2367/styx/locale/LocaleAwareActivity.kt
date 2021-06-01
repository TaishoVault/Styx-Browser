package com.jamal2367.styx.locale

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import com.jamal2367.styx.locale.LocaleUtils.requestedLocale
import com.jamal2367.styx.locale.LocaleUtils.updateLocale
import com.jamal2367.styx.preference.UserPreferences
import java.util.*
import javax.inject.Inject


abstract class LocaleAwareActivity : AppCompatActivity() {

    lateinit var mLastLocale: Locale

    @Inject
    lateinit var userPreferences: UserPreferences

    /**
     * Is called whenever the application locale has changed. Your Activity must either update
     * all localised Strings, or replace itself with an updated version.
     */
    abstract fun onLocaleChanged()
    override fun onCreate(savedInstanceState: Bundle?) {
        mLastLocale = requestedLocale(userPreferences.locale)
        updateLocale(this, mLastLocale)
        setLayoutDirection(window.decorView, mLastLocale)
        super.onCreate(savedInstanceState)
    }

    /**
     * Upon configuration change our new config is reset to system locale.
     * Locale.geDefault is also reset to system local apparently.
     * That's also true if locale was previously change on the application context.
     * Therefore we don't bother with application context for now.
     *
     * @param newConfig
     */
    @Suppress("DEPRECATION")
    override fun onConfigurationChanged(newConfig: Configuration) {
        val requestedLocale = requestedLocale(userPreferences.locale)
        Log.d(TAG, "Config changed - Last locale: $mLastLocale")
        Log.d(TAG, "Config changed - Requested locale: $requestedLocale")
        Log.d(TAG, "Config changed - New config locale (ignored): " + newConfig.locale)

        // Check if our request local was changed
        if (requestedLocale == mLastLocale) {
            // Requested locale is the same make sure we apply it anew as it was reset in our new config
            updateLocale(this, mLastLocale)
            setLayoutDirection(window.decorView, mLastLocale)
        } else {
            // Requested locale was changed, we will need to restart our activity then
            localeChanged(requestedLocale)
        }
        super.onConfigurationChanged(newConfig)
    }

    /**
     *
     * @param aNewLocale
     */
    fun localeChanged(aNewLocale: Locale) {
        Log.d(TAG, "Apply locale: $aNewLocale")
        mLastLocale = aNewLocale
        onLocaleChanged()
    }

    override fun onResume() {
        super.onResume()
        val requestedLocale = requestedLocale(
            userPreferences.locale
        )
        Log.d(TAG, "Resume - Last locale: $mLastLocale")
        Log.d(TAG, "Resume - Requested locale: $requestedLocale")

        // Check if locale was changed as we were paused, apply new locale as needed
        if (requestedLocale != mLastLocale) {
            localeChanged(requestedLocale)
        }
    }

    companion object {
        private const val TAG = "LocaleAwareActivity"

        /**
         * Force set layout direction to RTL or LTR by Locale.
         *
         * @param view
         * @param locale
         */
        fun setLayoutDirection(view: View?, locale: Locale?) {
            when (TextUtilsCompat.getLayoutDirectionFromLocale(locale)) {
                ViewCompat.LAYOUT_DIRECTION_RTL -> ViewCompat.setLayoutDirection(
                    view!!, ViewCompat.LAYOUT_DIRECTION_RTL
                )
                ViewCompat.LAYOUT_DIRECTION_LTR -> ViewCompat.setLayoutDirection(
                    view!!,
                    ViewCompat.LAYOUT_DIRECTION_LTR
                )
                else -> ViewCompat.setLayoutDirection(view!!, ViewCompat.LAYOUT_DIRECTION_LTR)
            }
        }
    }
}