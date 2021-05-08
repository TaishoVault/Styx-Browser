package com.jamal2367.styx.proxy

import android.content.Context
import android.content.pm.PackageManager

class I2PInstallCheck(private val mContext: Context) {
    private val mUseDebug = false
    val isI2PAndroidInstalled: Boolean
        get() = mUseDebug && isAppInstalled(URI_I2P_ANDROID_DEBUG) ||
                isAppInstalled(URI_I2P_ANDROID) ||
                isAppInstalled(URI_I2P_ANDROID_DONATE) ||
                isAppInstalled(URI_I2P_ANDROID_LEGACY)

    private fun isAppInstalled(uri: String): Boolean {
        val pm = mContext.packageManager
        return try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        const val URI_I2P_ANDROID = "net.i2p.android"
        const val URI_I2P_ANDROID_DONATE = "net.i2p.android.donate"
        const val URI_I2P_ANDROID_LEGACY = "net.i2p.android.router"
        const val URI_I2P_ANDROID_DEBUG = "net.i2p.android.debug"
    }
}