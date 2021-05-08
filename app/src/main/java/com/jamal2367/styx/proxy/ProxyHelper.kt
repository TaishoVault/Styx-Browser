package com.jamal2367.styx.proxy

import android.content.Context
import android.content.Intent

interface ProxyHelper {
    fun isInstalled(context: Context?): Boolean
    fun requestStatus(context: Context?)
    fun requestStart(context: Context?): Boolean
    fun getInstallIntent(context: Context?): Intent?
    fun getStartIntent(context: Context?): Intent?
    val name: String?

    companion object {
        const val FDROID_PACKAGE_NAME = "org.fdroid.fdroid"
        const val PLAY_PACKAGE_NAME = "com.android.vending"

        /**
         * A request to Orbot to transparently start Tor services
         */
        const val ACTION_START = "android.intent.action.PROXY_START"

        /**
         * [Intent] send by Orbot with `ON/OFF/STARTING/STOPPING` status
         */
        const val ACTION_STATUS = "android.intent.action.PROXY_STATUS"

        /**
         * `String` that contains a status constant: [.STATUS_ON],
         * [.STATUS_OFF], [.STATUS_STARTING], or
         * [.STATUS_STOPPING]
         */
        const val EXTRA_STATUS = "android.intent.extra.PROXY_STATUS"
        const val EXTRA_PROXY_PORT_HTTP = "android.intent.extra.PROXY_PORT_HTTP"
        const val EXTRA_PROXY_PORT_SOCKS = "android.intent.extra.PROXY_PORT_SOCKS"

        /**
         * A [String] `packageName` for Orbot to direct its status reply
         * to, used in [.ACTION_START] [Intent]s sent to Orbot
         */
        const val EXTRA_PACKAGE_NAME = "android.intent.extra.PROXY_PACKAGE_NAME"

        /**
         * All tor-related services and daemons have completed starting
         */
        const val STATUS_ON = "ON"
        const val STATUS_STARTING = "STARTING"
        const val STATUS_STOPPING = "STOPPING"

        /**
         * The user has disabled the ability for background starts triggered by
         * apps. Fallback to the old Intent that brings up Orbot.
         */
        const val STATUS_STARTS_DISABLED = "STARTS_DISABLED"
    }
}