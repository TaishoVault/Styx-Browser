package com.jamal2367.styx.locale

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import com.jamal2367.styx.locale.Locales.initializeLocale
import java.util.*

abstract class LocaleAwareAppCompatActivity : AppCompatActivity() {
    @Volatile
    private var mLastLocale: Locale? = null

    /**
     * Is called whenever the application locale has changed. Your Activity must either update
     * all localised Strings, or replace itself with an updated version.
     */
    abstract fun applyLocale()
    override fun onCreate(savedInstanceState: Bundle?) {
        initializeLocale(this)
        mLastLocale = LocaleManager.getInstance().getCurrentLocale(applicationContext)
        LocaleManager.getInstance().updateConfiguration(this, mLastLocale)

/*
        if (Settings.getInstance(this).shouldUseSecureMode()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
*/super.onCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val localeManager = LocaleManager.getInstance()
        localeManager.correctLocale(this, resources, resources.configuration)
        val changed =
            localeManager.onSystemConfigurationChanged(this, resources, newConfig, mLastLocale)
        if (changed != null) {
            LocaleManager.getInstance().updateConfiguration(this, changed)
            applyLocale()
            setLayoutDirection(window.decorView, changed)
        }
        super.onConfigurationChanged(newConfig)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onConfigurationChanged(resources.configuration)
    }

    override fun onResume() {
        super.onResume()
        (applicationContext as LocaleAwareApplication).onActivityResume()

        // Check if locale was changed as we were paused, apply new locale as needed
        if (mLastLocale !== LocaleManager.getInstance().getCurrentLocale(applicationContext)) {
            applyLocale()
        }
    }

    override fun onPause() {
        super.onPause()
        (applicationContext as LocaleAwareApplication).onActivityPause()
    }

    companion object {
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