package com.jamal2367.styx.utils

import android.app.Dialog
import android.content.DialogInterface
import android.util.Log
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamal2367.styx.R
import com.jamal2367.styx.browser.ProxyChoice
import com.jamal2367.styx.dialog.BrowserDialog.setDialogSize
import com.jamal2367.styx.extensions.snackbar
import com.jamal2367.styx.extensions.withSingleChoiceItems
import com.jamal2367.styx.preference.DeveloperPreferences
import com.jamal2367.styx.preference.UserPreferences
import com.jamal2367.styx.proxy.I2PInstallCheck
import com.jamal2367.styx.proxy.OrbotHelper
import com.jamal2367.styx.proxy.WebkitProxy
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ProxyUtils @Inject constructor(
        private val userPreferences: UserPreferences,
        private val developerPreferences: DeveloperPreferences) {

    fun checkForProxy(activity: AppCompatActivity) {
        val currentProxyChoice = userPreferences.proxyChoice
        val orbotInstalled = OrbotHelper.isOrbotInstalled(activity)
        val orbotChecked = developerPreferences.checkedForTor
        val orbot = orbotInstalled && !orbotChecked

        // Do only once per install
        if (currentProxyChoice !== ProxyChoice.NONE && (orbot)) {
            if (orbot) {
                developerPreferences.checkedForTor = true
            }
            val builder = MaterialAlertDialogBuilder(activity)
            if (orbotInstalled) {
                val proxyChoices = activity.resources.getStringArray(R.array.proxy_choices_array)
                val values = listOf(ProxyChoice.NONE, ProxyChoice.ORBOT, ProxyChoice.I2P)
                val list: MutableList<Pair<ProxyChoice, String>> = ArrayList()
                for (proxyChoice in values) {
                    list.add(Pair(proxyChoice, proxyChoices[proxyChoice.value]))
                }
                builder.setTitle(activity.resources.getString(R.string.http_proxy))
                builder.withSingleChoiceItems(list, userPreferences.proxyChoice, { newProxyChoice: ProxyChoice? ->
                    userPreferences.proxyChoice = newProxyChoice!!
                })
                builder.setPositiveButton(activity.resources.getString(R.string.action_ok)
                ) { _: DialogInterface?, _: Int ->
                    if (userPreferences.proxyChoice !== ProxyChoice.NONE) {
                        initializeProxy(activity)
                    }
                }
            } else {
                val dialogClickListener = DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            userPreferences.proxyChoice
                        }
                        DialogInterface.BUTTON_NEGATIVE -> userPreferences.proxyChoice = ProxyChoice.NONE
                    }
                }
                builder.setMessage(if (orbotInstalled) R.string.use_tor_prompt else R.string.use_i2p_prompt)
                        .setPositiveButton(R.string.yes, dialogClickListener)
                        .setNegativeButton(R.string.no, dialogClickListener)
            }
            val dialog: Dialog = builder.show()
            setDialogSize(activity, dialog)
        }
    }

    /*
     * Initialize WebKit Proxying
     */
    @Suppress("DEPRECATION")
    private fun initializeProxy(activity: AppCompatActivity) {
        val host: String
        val port: Int
        when (userPreferences.proxyChoice) {
            ProxyChoice.NONE ->
                return
            ProxyChoice.ORBOT -> {
                if (!OrbotHelper.isOrbotRunning(activity)) {
                    OrbotHelper.requestStartTor(activity)
                }
                host = "localhost"
                port = 8118
            }
            ProxyChoice.I2P -> {
                host = "localhost"
                port = 4444
            }
            ProxyChoice.MANUAL -> {
                host = userPreferences.proxyHost
                port = userPreferences.proxyPort
            }
        }
        try {
            WebkitProxy.setProxy(
                activity.applicationContext,
                host,
                port
            )
        } catch (e: Exception) {
            Log.d(TAG, "error enabling web proxying", e)
        }
    }

    fun isProxyReady(): Boolean {
        return true
    }

    fun updateProxySettings(activity: AppCompatActivity) {
        if (userPreferences.proxyChoice !== ProxyChoice.NONE) {
            initializeProxy(activity)
        } else {
            try {
                WebkitProxy.resetProxy(activity.applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to reset proxy", e)
            }
        }
    }

    fun onStop() {

    }

    fun onStart() {
    }

    @Suppress("NAME_SHADOWING")
    companion object {
        private const val TAG = "ProxyUtils"

        fun sanitizeProxyChoice(choice: ProxyChoice?, activity: AppCompatActivity): ProxyChoice? {
            var choice = choice
            when (choice) {
                ProxyChoice.ORBOT -> if (!OrbotHelper.isOrbotInstalled(activity)) {
                    choice = ProxyChoice.NONE
                    activity.snackbar(R.string.install_orbot, Gravity.BOTTOM)
                }
                ProxyChoice.I2P -> {
                    val ih = I2PInstallCheck(activity.application)
                    if (!ih.isI2PAndroidInstalled) {
                        choice = ProxyChoice.NONE
                        activity.snackbar(R.string.install_i2p, Gravity.BOTTOM)
                    }
                }
                ProxyChoice.MANUAL -> {
                }
                ProxyChoice.NONE ->{
                }
            }
            return choice
        }
    }
}