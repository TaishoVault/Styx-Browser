package com.jamal2367.styx.locale

import android.content.res.Resources.NotFoundException
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import androidx.preference.PreferenceFragmentCompat
import java.util.*

/**
 * SL: Should we use this too?
 */
abstract class LocaleAwarePreferenceFragment : PreferenceFragmentCompat() {
    private var cachedLocale: Locale? = null
    private var animationSet: AnimationSet? = null
    fun cancelAnimation() {
        if (animationSet != null) {
            animationSet!!.duration = 0
            animationSet!!.cancel()
        }
    }

    /**
     * Is called whenever the application locale has changed. Your fragment must either update
     * all localised Strings, or replace itself with an updated version.
     */
    abstract fun applyLocale()

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        var animation = super.onCreateAnimation(transit, enter, nextAnim)
        if (animation == null && nextAnim != 0) {
            animation = try {
                AnimationUtils.loadAnimation(activity, nextAnim)
            } catch (e: NotFoundException) {
                return null
            }
        }
        return if (animation != null) {
            val animSet = AnimationSet(true)
            animSet.addAnimation(animation)
            animationSet = animSet
            animSet
        } else {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        LocaleManager.getInstance()
            .correctLocale(context, resources, resources.configuration)
        if (cachedLocale == null) {
            cachedLocale = Locale.getDefault()
        } else {
            var newLocale = LocaleManager.getInstance().getCurrentLocale(requireActivity().applicationContext)
            if (newLocale == null) {
                // Using system locale:
                newLocale = Locale.getDefault()
            }
            if (newLocale != cachedLocale) {
                cachedLocale = newLocale
                applyLocale()
            }
        }
    }
}
