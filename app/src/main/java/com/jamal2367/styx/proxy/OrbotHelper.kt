package com.jamal2367.styx.proxy

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.Collections.newSetFromMap

class OrbotHelper private constructor(context: Context) : ProxyHelper {
    override fun isInstalled(context: Context?): Boolean {
        return isOrbotInstalled(context)
    }

    @Suppress("deprecation")
    override fun requestStatus(context: Context?) {
        isOrbotRunning(context)
    }

    override fun requestStart(context: Context?): Boolean {
        return requestStartTor(context)
    }

    override fun getInstallIntent(context: Context?): Intent {
        return getOrbotInstallIntent(context)
    }

    @Suppress("deprecation")
    override fun getStartIntent(context: Context?): Intent {
        return orbotStartIntent
    }

    override val name: String
        get() = "Orbot"

    /* MLM additions */
    private val context: Context = context.applicationContext
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var isInstalled = false
    private var lastStatusIntent: Intent? = null
    private val statusCallbacks = newSetFromMap(WeakHashMap<StatusCallback, Boolean?>())
    private val installCallbacks = newSetFromMap(WeakHashMap<InstallCallback, Boolean?>())
    private var statusTimeoutMs = 30000L
    private var installTimeoutMs = 60000L
    private var validateOrbot = true

    /**
     * Callback interface used for reporting the results of an
     * attempt to install Orbot
     */
    interface InstallCallback {
        fun onInstalled()
        fun onInstallTimeout()
    }

    /**
     * Adds a StatusCallback to be called when we find out that
     * Orbot is ready. If Orbot is ready for use, your callback
     * will be called with onEnabled() immediately, before this
     * method returns.
     *
     * @param cb a callback
     * @return the singleton, for chaining
     */
    fun addStatusCallback(cb: StatusCallback): OrbotHelper {
        statusCallbacks.add(cb)
        if (lastStatusIntent != null) {
            val status = lastStatusIntent!!.getStringExtra(EXTRA_STATUS)
            if (status == STATUS_ON) {
                cb.onEnabled(lastStatusIntent)
            }
        }
        return this
    }

    /**
     * Removes an existing registered StatusCallback.
     *
     * @param cb the callback to remove
     * @return the singleton, for chaining
     */
    fun removeStatusCallback(cb: StatusCallback): OrbotHelper {
        statusCallbacks.remove(cb)
        return this
    }

    /**
     * Adds an InstallCallback to be called when we find out that
     * Orbot is installed
     *
     * @param cb a callback
     * @return the singleton, for chaining
     */
    fun addInstallCallback(cb: InstallCallback): OrbotHelper {
        installCallbacks.add(cb)
        return this
    }

    /**
     * Removes an existing registered InstallCallback.
     *
     * @param cb the callback to remove
     * @return the singleton, for chaining
     */
    fun removeInstallCallback(cb: InstallCallback): OrbotHelper {
        installCallbacks.remove(cb)
        return this
    }

    /**
     * Sets how long of a delay, in milliseconds, after trying
     * to get a status from Orbot before we give up.
     * Defaults to 30000ms = 30 seconds = 0.000347222 days
     *
     * @param timeoutMs delay period in milliseconds
     * @return the singleton, for chaining
     */
    fun statusTimeout(timeoutMs: Long): OrbotHelper {
        statusTimeoutMs = timeoutMs
        return this
    }

    /**
     * Sets how long of a delay, in milliseconds, after trying
     * to install Orbot do we assume that it's not happening.
     * Defaults to 60000ms = 60 seconds = 1 minute = 1.90259e-6 years
     *
     * @param timeoutMs delay period in milliseconds
     * @return the singleton, for chaining
     */
    fun installTimeout(timeoutMs: Long): OrbotHelper {
        installTimeoutMs = timeoutMs
        return this
    }

    /**
     * By default, NetCipher ensures that the Orbot on the
     * device is one of the official builds. Call this method
     * to skip that validation. Mostly, this is for developers
     * who have their own custom Orbot builds (e.g., for
     * dedicated hardware).
     *
     * @return the singleton, for chaining
     */
    fun skipOrbotValidation(): OrbotHelper {
        validateOrbot = false
        return this
    }

    /**
     * @return true if Orbot is installed (the last time we checked),
     * false otherwise
     */
    fun isInstalled(): Boolean {
        return isInstalled
    }

    /**
     * Initializes the connection to Orbot, revalidating that it is installed
     * and requesting fresh status broadcasts.  This is best run in your app's
     * [android.app.Application] subclass, in its
     * [android.app.Application.onCreate] method.
     *
     * @return true if initialization is proceeding, false if Orbot is not installed,
     * or version of Orbot with a unofficial signing key is present.
     */
    fun init(): Boolean {
        val orbot: Intent = getOrbotStartIntent(context)
        if (validateOrbot) {
            val hashes = ArrayList<String>()
            // Tor Project signing key
            hashes.add("A4:54:B8:7A:18:47:A8:9E:D7:F5:E7:0F:BA:6B:BA:96:F3:EF:29:C2:6E:09:81:20:4F:E3:47:BF:23:1D:FD:5B")
            // f-droid.org signing key
            hashes.add("A7:02:07:92:4F:61:FF:09:37:1D:54:84:14:5C:4B:EE:77:2C:55:C1:9E:EE:23:2F:57:70:E1:82:71:F7:CB:AE")
        }
        isInstalled = true
        handler.postDelayed(onStatusTimeout, statusTimeoutMs)
        context.registerReceiver(
            orbotStatusReceiver,
            IntentFilter(ACTION_STATUS)
        )
        context.sendBroadcast(orbot)
        return isInstalled
    }

    /**
     * Given that init() returned false, calling installOrbot()
     * will trigger an attempt to install Orbot from an available
     * distribution channel (e.g., the Play Store). Only call this
     * if the user is expecting it, such as in response to tapping
     * a dialog button or an action bar item.
     *
     *
     * Note that installation may take a long time, even if
     * the user is proceeding with the installation, due to network
     * speeds, waiting for user input, and so on. Either specify
     * a long timeout, or consider the timeout to be merely advisory
     * and use some other user input to cause you to try
     * init() again after, presumably, Orbot has been installed
     * and configured by the user.
     *
     *
     * If the user does install Orbot, we will attempt init()
     * again automatically. Hence, you will probably need user input
     * to tell you when the user has gotten Orbot up and going.
     *
     * @param host the Activity that is triggering this work
     */
    fun installOrbot(host: Activity) {
        handler.postDelayed(onInstallTimeout, installTimeoutMs)
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
        filter.addDataScheme("package")
        context.registerReceiver(orbotInstallReceiver, filter)
        host.startActivity(getOrbotInstallIntent(context))
    }

    private val orbotStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (TextUtils.equals(
                    intent.action,
                    ACTION_STATUS
                )
            ) {
                when (intent.getStringExtra(EXTRA_STATUS)) {
                    STATUS_ON -> {
                        lastStatusIntent = intent
                        handler.removeCallbacks(onStatusTimeout)
                        for (cb in statusCallbacks) {
                            cb.onEnabled(intent)
                        }
                    }
                    STATUS_OFF -> {
                        for (cb in statusCallbacks) {
                            cb.onDisabled()
                        }
                    }
                    STATUS_STARTING -> {
                        for (cb in statusCallbacks) {
                            cb.onStarting()
                        }
                    }
                    STATUS_STOPPING -> {
                        for (cb in statusCallbacks) {
                            cb.onStopping()
                        }
                    }
                }
            }
        }
    }
    private val onStatusTimeout = Runnable {
        context.unregisterReceiver(orbotStatusReceiver)
        for (cb in statusCallbacks) {
            cb.onStatusTimeout()
        }
    }
    private val orbotInstallReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (TextUtils.equals(
                    intent.action,
                    Intent.ACTION_PACKAGE_ADDED
                )
            ) {
                val pkgName = intent.data!!.encodedSchemeSpecificPart
                if (ORBOT_PACKAGE_NAME == pkgName) {
                    isInstalled = true
                    handler.removeCallbacks(onInstallTimeout)
                    context.unregisterReceiver(this)
                    for (cb in installCallbacks) {
                        cb.onInstalled()
                    }
                    init()
                }
            }
        }
    }
    private val onInstallTimeout = Runnable {
        context.unregisterReceiver(orbotInstallReceiver)
        for (cb in installCallbacks) {
            cb.onInstallTimeout()
        }
    }

    companion object {
        const val ORBOT_PACKAGE_NAME = "org.torproject.android"
        private const val ORBOT_MARKET_URI = "market://details?id=$ORBOT_PACKAGE_NAME"
        private const val ORBOT_FDROID_URI = ("https://f-droid.org/repository/browse/?fdid=$ORBOT_PACKAGE_NAME")
        const val DEFAULT_PROXY_HOST = "localhost"
        const val DEFAULT_PROXY_SOCKS_PORT = 9050

        /**
         * A request to Orbot to transparently start Tor services
         */
        private const val ACTION_START = "org.torproject.android.intent.action.START"

        /**
         * [Intent] send by Orbot with `ON/OFF/STARTING/STOPPING` status
         * included as an [.EXTRA_STATUS] `String`.  Your app should
         * always receive `ACTION_STATUS Intent`s since any other app could
         * start Orbot.  Also, user-triggered starts and stops will also cause
         * `ACTION_STATUS Intent`s to be broadcast.
         */
        const val ACTION_STATUS = "org.torproject.android.intent.action.STATUS"

        /**
         * `String` that contains a status constant: [.STATUS_ON],
         * [.STATUS_OFF], [.STATUS_STARTING], or
         * [.STATUS_STOPPING]
         */
        const val EXTRA_STATUS = "org.torproject.android.intent.extra.STATUS"

        /**
         * A [String] `packageName` for Orbot to direct its status reply
         * to, used in [.ACTION_START] [Intent]s sent to Orbot
         */
        private const val EXTRA_PACKAGE_NAME = "org.torproject.android.intent.extra.PACKAGE_NAME"

        /**
         * All tor-related services and daemons are stopped
         */
        const val STATUS_OFF = "OFF"

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
        private const val ACTION_START_TOR = "org.torproject.android.START_TOR"

        /**
         * Intent Action to request V2 Onion Services
         * See {[.requestHiddenServiceOnPort]}
         */
        @Deprecated("")
        val ACTION_REQUEST_HS = "org.torproject.android.REQUEST_HS_PORT"

        /**
         * Intent Action to request V3 Onion Services
         * See {[.requestV3OnionServiceOnPort]}
         */
        private const val ACTION_REQUEST_V3_ONION_SERVICE = "org.torproject.android.REQUEST_V3_ONION_SERVICE"
        private const val START_TOR_RESULT = 0x9234
        private const val HS_REQUEST_CODE = 9999
        private const val V3_ONION_SERVICE_REQUEST_CODE = 3333
        /*
    private OrbotHelper() {
        // only static utility methods, do not instantiate
    }
*/
        /**
         * Test whether a [URL] is a Tor Onion Service host name, also known
         * as an ".onion address".
         *
         * @return whether the host name is a Tor .onion address
         */
        fun isOnionAddress(url: URL): Boolean {
            return url.host.endsWith(".onion")
        }

        /**
         * Test whether a URL [String] is a Tor Onion Service host name, also known
         * as an ".onion address".
         *
         * @return whether the host name is a Tor .onion address
         */
        fun isOnionAddress(urlString: String?): Boolean {
            return try {
                isOnionAddress(URL(urlString))
            } catch (e: MalformedURLException) {
                false
            }
        }

        /**
         * Test whether a [Uri] is a Tor Hidden Service host name, also known
         * as an ".onion address".
         *
         * @return whether the host name is a Tor .onion address
         */
        fun isOnionAddress(uri: Uri): Boolean {
            return uri.host!!.endsWith(".onion")
        }

        /**
         * Check if the tor process is running.  This method is very
         * brittle, and is therefore deprecated in favor of using the
         * [.ACTION_STATUS] `Intent` along with the
         * [.requestStartTor] method.
         */
        @Deprecated("")
        fun isOrbotRunning(context: Context?): Boolean {
            val procId = context?.let { TorServiceUtils.findProcessId(it) }
            return procId != -1
        }

        fun isOrbotInstalled(context: Context?): Boolean {
            return isAppInstalled(context)
        }

        private fun isAppInstalled(context: Context?): Boolean {
            return try {
                val pm = context!!.packageManager
                pm.getPackageInfo(ORBOT_PACKAGE_NAME, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        /**
         * This method creates a V2 Onion Service which is being phased out by tor soon.
         * Instead, you should use {[.requestV3OnionServiceOnPort]}
         * to create a V3 Onion Service. See https://blog.torproject.org/v2-deprecation-timeline
         */
        @Suppress("deprecation")
        fun requestHiddenServiceOnPort(activity: Activity, port: Int) {
            val intent = Intent(ACTION_REQUEST_HS)
            intent.setPackage(ORBOT_PACKAGE_NAME)
            intent.putExtra("hs_port", port)
            activity.startActivityForResult(intent, HS_REQUEST_CODE)
        }

        /**
         * Tells Orbot to spin up a v3 Onion Service for your application
         * @param port the local port that your service is running on
         * @param onionPort the virtual port for the onion service to use
         * @param orbotServiceName a labeling string that will be displayed in Orbot
         */
        fun requestV3OnionServiceOnPort(
            activity: Activity,
            port: Int,
            onionPort: Int,
            orbotServiceName: String?
        ) {
            val intent = Intent(ACTION_REQUEST_V3_ONION_SERVICE)
            intent.setPackage(ORBOT_PACKAGE_NAME)
            intent.putExtra("localPort", port)
            intent.putExtra("onionPort", onionPort)
            intent.putExtra("name", orbotServiceName)
            activity.startActivityForResult(intent, V3_ONION_SERVICE_REQUEST_CODE)
        }

        /**
         * First, checks whether Orbot is installed. If Orbot is installed, then a
         * broadcast [Intent] is sent to request Orbot to start
         * transparently in the background. When Orbot receives this `Intent`, it will immediately reply to the app that called this method
         * with an [.ACTION_STATUS] `Intent` that is broadcast to the
         * `packageName` of the provided [Context] (i.e.  [ ][Context.getPackageName].
         *
         *
         * That reply [.ACTION_STATUS] `Intent` could say that the user
         * has disabled background starts with the status
         * [.STATUS_STARTS_DISABLED]. That means that Orbot ignored this
         * request.  To directly prompt the user to start Tor, use
         * [.requestShowOrbotStart], which will bring up
         * Orbot itself for the user to manually start Tor.  Orbot always broadcasts
         * it's status, so your app will receive those no matter how Tor gets
         * started.
         *
         * @param context the app [Context] will receive the reply
         * @return whether the start request was sent to Orbot
         * @see .requestShowOrbotStart
         */
        fun requestStartTor(context: Context?): Boolean {
            if (isOrbotInstalled(context)) {
                Log.i("OrbotHelper", "requestStartTor " + context!!.packageName)
                val intent = getOrbotStartIntent(context)
                context.sendBroadcast(intent)
                return true
            }
            return false
        }

        /**
         * Gets an [Intent] for starting Orbot.  Orbot will reply with the
         * current status to the `packageName` of the app in the provided
         * [Context] (i.e.  [Context.getPackageName].
         */
        fun getOrbotStartIntent(context: Context?): Intent {
            val intent = Intent(ACTION_START)
            intent.setPackage(ORBOT_PACKAGE_NAME)
            intent.putExtra(EXTRA_PACKAGE_NAME, context!!.packageName)
            return intent
        }

        /**
         * Gets a barebones [Intent] for starting Orbot.  This is deprecated
         * in favor of [.getOrbotStartIntent].
         */
        @get:Deprecated("")
        val orbotStartIntent: Intent
            get() {
                val intent = Intent(ACTION_START)
                intent.setPackage(ORBOT_PACKAGE_NAME)
                return intent
            }

        @Suppress("deprecation")
        fun requestShowOrbotStart(activity: Activity): Boolean {
            if (isOrbotInstalled(activity)) {
                if (!isOrbotRunning(activity)) {
                    val intent = showOrbotStartIntent
                    activity.startActivityForResult(intent, START_TOR_RESULT)
                    return true
                }
            }
            return false
        }

        val showOrbotStartIntent: Intent
            get() {
                val intent = Intent(ACTION_START_TOR)
                intent.setPackage(ORBOT_PACKAGE_NAME)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return intent
            }

        fun getOrbotInstallIntent(context: Context?): Intent {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(ORBOT_MARKET_URI)
            val pm = context!!.packageManager
            val resInfos = pm.queryIntentActivities(intent, 0)
            var foundPackageName: String? = null
            for (r in resInfos) {
                Log.i("OrbotHelper", "market: " + r.activityInfo.packageName)
                if (TextUtils.equals(r.activityInfo.packageName, ProxyHelper.FDROID_PACKAGE_NAME)
                    || TextUtils.equals(r.activityInfo.packageName, ProxyHelper.PLAY_PACKAGE_NAME)
                ) {
                    foundPackageName = r.activityInfo.packageName
                    break
                }
            }
            if (foundPackageName == null) {
                intent.data = Uri.parse(ORBOT_FDROID_URI)
            } else {
                intent.setPackage(foundPackageName)
            }
            return intent
        }

        @Volatile
        private var instance: OrbotHelper? = null

        /**
         * Retrieves the singleton, initializing if if needed
         *
         * @param context any Context will do, as we will hold onto
         * the Application
         * @return the singleton
         */
        @Synchronized
        operator fun get(context: Context): OrbotHelper? {
            if (instance == null) {
                instance = OrbotHelper(context)
            }
            return instance
        }


    }

}