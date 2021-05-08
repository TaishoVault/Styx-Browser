package com.jamal2367.styx.proxy

import android.content.Intent

/**
 * Callback interface used for reporting Orbot status
 */
interface StatusCallback {
    /**
     * Called when Orbot is operational
     *
     * @param statusIntent an Intent containing information about
     * Orbot, including proxy ports
     */
    fun onEnabled(statusIntent: Intent?)

    /**
     * Called when Orbot reports that it is starting up
     */
    fun onStarting()

    /**
     * Called when Orbot reports that it is shutting down
     */
    fun onStopping()

    /**
     * Called when Orbot reports that it is no longer running
     */
    fun onDisabled()

    /**
     * Called if our attempt to get a status from Orbot failed
     * after a defined period of time. See statusTimeout() on
     * OrbotInitializer.
     */
    fun onStatusTimeout()

    /**
     * Called if Orbot is not yet installed. Usually, you handle
     * this by checking the return value from init() on OrbotInitializer
     * or calling isInstalled() on OrbotInitializer. However, if
     * you have need for it, if a callback is registered before
     * an init() call determines that Orbot is not installed, your
     * callback will be called with onNotYetInstalled().
     */
    fun onNotYetInstalled()
}