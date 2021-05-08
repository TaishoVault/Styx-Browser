package com.jamal2367.styx

import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.jamal2367.styx.di.injector
import com.jamal2367.styx.preference.UserPreferences
import com.jamal2367.styx.utils.ThemeUtils
import javax.inject.Inject

abstract class ThemedActivity : AppCompatActivity() {

    @Inject lateinit var userPreferences: UserPreferences

    protected var themeId: AppTheme = AppTheme.LIGHT
    private var isDarkTheme: Boolean = false
    val useDarkTheme get() = isDarkTheme

    /**
     * Override this to provide an alternate theme that should be set for every instance of this
     * activity regardless of the user's preference.
     */
    protected open fun provideThemeOverride(): AppTheme? = null

    /**
     * Called after the activity is resumed
     * and the UI becomes visible to the user.
     * Called by onWindowFocusChanged only if
     * onResume has been called.
     */
    protected open fun onWindowVisibleToUserAfterResume() = Unit

    /**
     * Implement this to provide themes resource style ids.
     */
    @StyleRes
    abstract fun themeStyle(aTheme: AppTheme): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        themeId = userPreferences.useTheme

        // set the theme
        applyTheme(provideThemeOverride()?:themeId)
        applyAccent()
        super.onCreate(savedInstanceState)
        resetPreferences()
    }

    /**
     *
     */
    protected fun resetPreferences() {
        if (userPreferences.useBlackStatusBar) {
            window.statusBarColor = Color.BLACK
        } else {
            window.statusBarColor = ThemeUtils.getStatusBarColor(this)
        }
    }

    /**
     *
     */
    protected fun applyTheme(themeId: AppTheme) {
        setTheme(themeStyle(themeId))
        // Check if we have a dark theme
        val mode = resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
        isDarkTheme = themeId == AppTheme.BLACK // Black qualifies as dark theme
                || themeId == AppTheme.DARK // Dark is indeed a dark theme
                // Check if we are using system default theme and it is currently set to dark
                || (themeId == AppTheme.DEFAULT && mode == Configuration.UI_MODE_NIGHT_YES)
    }

    /**
     *
     */
    protected fun applyAccent() {
        val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        when (pref.getString("pref_key_accent_list", "Accent_Color")) {
            "pref_key_accent_default" -> {
                userPreferences.useTheme
            }
            "pref_key_accent_pink" -> {
                theme.applyStyle(R.style.Accent_Pink, true)
            }
            "pref_key_accent_purple" -> {
                theme.applyStyle(R.style.Accent_Puple, true)
            }
            "pref_key_accent_deep_purple" -> {
                theme.applyStyle(R.style.Accent_Deep_Purple, true)
            }
            "pref_key_accent_indigo" -> {
                theme.applyStyle(R.style.Accent_Indigo, true)
            }
            "pref_key_accent_blue" -> {
                theme.applyStyle(R.style.Accent_Blue, true)
            }
            "pref_key_accent_light_blue" -> {
                theme.applyStyle(R.style.Accent_Light_Blue, true)
            }
            "pref_key_accent_cyan" -> {
                theme.applyStyle(R.style.Accent_Cyan, true)
            }
            "pref_key_accent_teal" -> {
                theme.applyStyle(R.style.Accent_Teal, true)
            }
            "pref_key_accent_green" -> {
                theme.applyStyle(R.style.Accent_Green, true)
            }
            "pref_key_accent_light_green" -> {
                theme.applyStyle(R.style.Accent_Light_Green, true)
            }
            "pref_key_accent_lime" -> {
                theme.applyStyle(R.style.Accent_Lime, true)
            }
            "pref_key_accent_yellow" -> {
                theme.applyStyle(R.style.Accent_Yellow, true)
            }
            "pref_key_accent_amber" -> {
                theme.applyStyle(R.style.Accent_Amber, true)
            }
            "pref_key_accent_orange" -> {
                theme.applyStyle(R.style.Accent_Orange, true)
            }
            "pref_key_accent_deep_orange" -> {
                theme.applyStyle(R.style.Accent_Deep_Orange, true)
            }
            "pref_key_accent_brown" -> {
                theme.applyStyle(R.style.Accent_Brown, true)
            }
        }
    }

}
