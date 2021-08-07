package com.jamal2367.styx.utils

import android.content.Context
import android.graphics.Point
import android.view.WindowManager

/**
 * Utils used to access information about the device.
 */
object DeviceUtils {

    /**
     * Gets the width of the device's screen.
     *
     * @param context the context used to access the [WindowManager].
     */
    @Suppress("DEPRECATION")
    @JvmStatic
    fun getScreenWidth(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return Point().apply {
            windowManager.defaultDisplay.getSize(this)
        }.x
    }

}
