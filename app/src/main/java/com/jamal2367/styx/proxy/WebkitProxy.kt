package com.jamal2367.styx.proxy

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Proxy
import android.net.Uri
import android.os.Parcelable
import android.util.ArrayMap
import android.util.Log
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.InetSocketAddress
import java.net.Socket

object WebkitProxy {
    private const val REQUEST_CODE = 0

    @Throws(Exception::class)
    fun setProxy(
        ctx: Context,
        host: String,
        port: Int
    ): Boolean {
        setSystemProperties(host, port)
        return setWebkitProxyLollipop(ctx, host, port)
    }

    private fun setSystemProperties(host: String, port: Int) {
        System.setProperty("proxyHost", host)
        System.setProperty("proxyPort", port.toString())
        System.setProperty("http.proxyHost", host)
        System.setProperty("http.proxyPort", port.toString())
        System.setProperty("https.proxyHost", host)
        System.setProperty("https.proxyPort", port.toString())
        System.setProperty("socks.proxyHost", host)
        System.setProperty(
            "socks.proxyPort",
            OrbotHelper.DEFAULT_PROXY_SOCKS_PORT.toString()
        )
        System.setProperty("socksProxyHost", host)
        System.setProperty("socksProxyPort", OrbotHelper.DEFAULT_PROXY_SOCKS_PORT.toString())
    }

    private fun resetSystemProperties() {
        System.setProperty("proxyHost", "")
        System.setProperty("proxyPort", "")
        System.setProperty("http.proxyHost", "")
        System.setProperty("http.proxyPort", "")
        System.setProperty("https.proxyHost", "")
        System.setProperty("https.proxyPort", "")
        System.setProperty("socks.proxyHost", "")
        System.setProperty(
            "socks.proxyPort",
            OrbotHelper.DEFAULT_PROXY_SOCKS_PORT.toString()
        )
        System.setProperty("socksProxyHost", "")
        System.setProperty("socksProxyPort", OrbotHelper.DEFAULT_PROXY_SOCKS_PORT.toString())
    }

    @TargetApi(21)
    fun resetLollipopProxy(appContext: Context): Boolean {
        return setWebkitProxyLollipop(appContext, null, 0)
    }

    // http://stackanswers.com/questions/25272393/android-webview-set-proxy-programmatically-on-android-l
    @SuppressLint("PrivateApi")
    @TargetApi(21) // for android.util.ArrayMap methods
    private fun setWebkitProxyLollipop(appContext: Context, host: String?, port: Int): Boolean {
        try {
            val applictionClass = Class.forName("android.app.Application")
            val mLoadedApkField = applictionClass.getDeclaredField("mLoadedApk")
            mLoadedApkField.isAccessible = true
            val mloadedApk = mLoadedApkField[appContext]
            val loadedApkClass = Class.forName("android.app.LoadedApk")
            val mReceiversField = loadedApkClass.getDeclaredField("mReceivers")
            mReceiversField.isAccessible = true
            val receivers = mReceiversField[mloadedApk] as ArrayMap<*, *>
            for (receiverMap in receivers.values) {
                for (receiver in (receiverMap as ArrayMap<*, *>).keys) {
                    val clazz: Class<*> = receiver.javaClass
                    if (clazz.name.contains("ProxyChangeListener")) {
                        val onReceiveMethod = clazz.getDeclaredMethod(
                            "onReceive",
                            Context::class.java,
                            Intent::class.java
                        )
                        val intent = Intent(Proxy.PROXY_CHANGE_ACTION)
                        var proxyInfo: Any? = null
                        if (host != null) {
                            val classname = "android.net.ProxyInfo"
                            val cls = Class.forName(classname)
                            val buildDirectProxyMethod =
                                cls.getMethod("buildDirectProxy", String::class.java, Integer.TYPE)
                            proxyInfo = buildDirectProxyMethod.invoke(cls, host, port)
                        }
                        intent.putExtra("proxy", proxyInfo as Parcelable?)
                        onReceiveMethod.invoke(receiver, appContext, intent)
                    }
                }
            }
            return true
        } catch (e: ClassNotFoundException) {
            Log.d(
                "ProxySettings",
                "Exception setting WebKit proxy on Lollipop through ProxyChangeListener: $e"
            )
        } catch (e: NoSuchFieldException) {
            Log.d(
                "ProxySettings",
                "Exception setting WebKit proxy on Lollipop through ProxyChangeListener: $e"
            )
        } catch (e: IllegalAccessException) {
            Log.d(
                "ProxySettings",
                "Exception setting WebKit proxy on Lollipop through ProxyChangeListener: $e"
            )
        } catch (e: NoSuchMethodException) {
            Log.d(
                "ProxySettings",
                "Exception setting WebKit proxy on Lollipop through ProxyChangeListener: $e"
            )
        } catch (e: InvocationTargetException) {
            Log.d(
                "ProxySettings",
                "Exception setting WebKit proxy on Lollipop through ProxyChangeListener: $e"
            )
        }
        return false
    }

    @Throws(Exception::class)
    fun resetProxy(ctx: Context): Boolean {
        resetSystemProperties()
        return resetLollipopProxy(ctx)
    }

    @SuppressLint("PrivateApi")
    @Throws(Exception::class)
    fun getRequestQueue(ctx: Context): Any? {
        var ret: Any? = null
        val networkClass = Class.forName("android.webkit.Network")
        val networkObj = invokeMethod(
            networkClass, "getInstance", arrayOf(
                ctx
            ), Context::class.java
        )
        if (networkObj != null) {
            ret = getDeclaredField(networkObj, "mRequestQueue")
        }
        return ret
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    private fun getDeclaredField(obj: Any, name: String): Any {
        val f = obj.javaClass.getDeclaredField(name)
        f.isAccessible = true
        return f[obj]
    }

    @Throws(Exception::class)
    private fun invokeMethod(
        `object`: Any, methodName: String, params: Array<Any>,
        vararg types: Class<*>
    ): Any? {
        val out: Any?
        val c = if (`object` is Class<*>) `object` else `object`.javaClass
        out = run {
            val method = c.getMethod(methodName, *types)
            method.invoke(`object`, *params)
        }
        return out
    }

    @Throws(IOException::class)
    fun getSocket(proxyHost: String?, proxyPort: Int): Socket {
        val sock = Socket()
        sock.connect(InetSocketAddress(proxyHost, proxyPort), 10000)
        return sock
    }

    @Throws(IOException::class)
    fun getSocket(): Socket {
        return getSocket(
            OrbotHelper.DEFAULT_PROXY_HOST,
            OrbotHelper.DEFAULT_PROXY_SOCKS_PORT
        )
    }

    fun initOrbot(
        activity: Activity,
        stringTitle: CharSequence,
        stringMessage: CharSequence,
        stringButtonYes: CharSequence,
        stringButtonNo: CharSequence
    ): AlertDialog? {
        val intentScan = Intent("org.torproject.android.START_TOR")
        intentScan.addCategory(Intent.CATEGORY_DEFAULT)
        return try {
            activity.startActivityForResult(intentScan, REQUEST_CODE)
            null
        } catch (e: ActivityNotFoundException) {
            showDownloadDialog(
                activity, stringTitle, stringMessage, stringButtonYes,
                stringButtonNo
            )
        }
    }

    private fun showDownloadDialog(
        activity: Activity,
        stringTitle: CharSequence,
        stringMessage: CharSequence,
        stringButtonYes: CharSequence,
        stringButtonNo: CharSequence
    ): AlertDialog {
        val downloadDialog = AlertDialog.Builder(activity)
        downloadDialog.setTitle(stringTitle)
        downloadDialog.setMessage(stringMessage)
        downloadDialog.setPositiveButton(stringButtonYes) { _, _ ->
            val uri = Uri.parse("market://search?q=pname:org.torproject.android")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activity.startActivity(intent)
        }
        downloadDialog.setNegativeButton(stringButtonNo) { _, _ -> }
        return downloadDialog.show()
    }
}