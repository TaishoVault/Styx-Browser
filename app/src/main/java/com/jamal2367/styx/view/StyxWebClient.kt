package com.jamal2367.styx.view

import com.jamal2367.styx.BuildConfig
import com.jamal2367.styx.R
import com.jamal2367.styx.adblock.AdBlocker
import com.jamal2367.styx.adblock.allowlist.AllowListModel
import com.jamal2367.styx.browser.activity.BrowserActivity
import com.jamal2367.styx.constant.FILE
import com.jamal2367.styx.controller.UIController
import com.jamal2367.styx.di.UserPrefs
import com.jamal2367.styx.di.injector
import com.jamal2367.styx.extensions.resizeAndShow
import com.jamal2367.styx.extensions.snackbar
import com.jamal2367.styx.js.InvertPage
import com.jamal2367.styx.js.SetMetaViewport
import com.jamal2367.styx.js.TextReflow
import com.jamal2367.styx.log.Logger
import com.jamal2367.styx.preference.UserPreferences
import com.jamal2367.styx.ssl.SslState
import com.jamal2367.styx.ssl.SslWarningPreferences
import com.jamal2367.styx.utils.IntentUtils
import com.jamal2367.styx.utils.ProxyUtils
import com.jamal2367.styx.utils.Utils
import com.jamal2367.styx.utils.isSpecialUrl
import com.jamal2367.styx.view.StyxView.Companion.KFetchMetaThemeColorTries
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.MailTo
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.LayoutInflater
import android.webkit.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URISyntaxException
import java.net.URL
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

class StyxWebClient(
    private val activity: Activity,
    private val styxView: StyxView
) : WebViewClient() {

    private val uiController: UIController
    private val intentUtils = IntentUtils(activity)
    private val emptyResponseByteArray: ByteArray = byteArrayOf()

    @Inject internal lateinit var proxyUtils: ProxyUtils
    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject @UserPrefs internal lateinit var preferences: SharedPreferences
    @Inject internal lateinit var sslWarningPreferences: SslWarningPreferences
    @Inject internal lateinit var whitelistModel: AllowListModel
    @Inject internal lateinit var logger: Logger
    @Inject internal lateinit var textReflowJs: TextReflow
    @Inject internal lateinit var invertPageJs: InvertPage
    @Inject internal lateinit var setMetaViewport: SetMetaViewport

    private var adBlock: AdBlocker

    private var urlWithSslError: String? = null

    @Volatile private var isRunning = false
    private var zoomScale = 0.0f

    private var currentUrl: String = ""

    var sslState: SslState = SslState.None
        private set(value) {
            sslStateSubject.onNext(value)
            field = value
        }

    private val sslStateSubject: PublishSubject<SslState> = PublishSubject.create()

    init {
        activity.injector.inject(this)
        uiController = activity as UIController
        adBlock = chooseAdBlocker()
    }

    fun sslStateObservable(): Observable<SslState> = sslStateSubject.hide()

    fun updatePreferences() {
        adBlock = chooseAdBlocker()
    }

    private fun chooseAdBlocker(): AdBlocker = if (userPreferences.adBlockEnabled) {
        activity.injector.provideBloomFilterAdBlocker()
    } else {
        activity.injector.provideNoOpAdBlocker()
    }

    private fun shouldRequestBeBlocked(pageUrl: String, requestUrl: String) =
        !whitelistModel.isUrlAllowedAds(pageUrl) && adBlock.isAd(requestUrl)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (shouldRequestBeBlocked(currentUrl, request.url.toString())) {
            val empty = ByteArrayInputStream(emptyResponseByteArray)
            return WebResourceResponse("text/plain", "utf-8", empty)
        }
        return super.shouldInterceptRequest(view, request)
    }

    @Suppress("OverridingDeprecatedMember")
    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        if (shouldRequestBeBlocked(currentUrl, url)) {
            val empty = ByteArrayInputStream(emptyResponseByteArray)
            return WebResourceResponse("text/plain", "utf-8", empty)
        }
        return null
    }

    override fun onLoadResource(view: WebView, url: String?) {
        super.onLoadResource(view, url)
        if (styxView.toggleDesktop) {
            // That's needed for desktop mode support
            // See: https://stackoverflow.com/a/60621350/3969362
            // See: https://stackoverflow.com/a/39642318/3969362
            // Note how we compute our initial scale to be zoomed out and fit the page
            // TODO: Check if we really need this here in onLoadResource
            // Pick the proper settings desktop width according to current orientation
            (Resources.getSystem().configuration.orientation == Configuration.ORIENTATION_PORTRAIT).let{portrait ->
                view.evaluateJavascript(setMetaViewport.provideJs().replaceFirst("\$width\$",(if (portrait) userPreferences.desktopWidthInPortrait else userPreferences.desktopWidthInLandscape).toString()),null)
            }
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (view.isShown) {
            uiController.updateUrl(url, false)
            uiController.setBackButtonEnabled(view.canGoBack())
            uiController.setForwardButtonEnabled(view.canGoForward())
            view.postInvalidate()
        }
        if (view.title == null || view.title!!.isEmpty()) {
            styxView.titleInfo.setTitle(activity.getString(R.string.untitled))
        } else {
            styxView.titleInfo.setTitle(view.title)
        }
        if (styxView.invertPage) {
            view.evaluateJavascript(invertPageJs.provideJs(), null)
        }

        uiController.tabChanged(styxView)
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        currentUrl = url
        // Only set the SSL state if there isn't an error for the current URL.
        if (urlWithSslError != url) {
            sslState = if (URLUtil.isHttpsUrl(url)) {
                SslState.Valid
            } else {
                SslState.None
            }
        }
        styxView.titleInfo.setFavicon(null)
        if (styxView.isShown) {
            uiController.updateUrl(url, true)
            uiController.showActionBar()
        }

        // Try to fetch meta theme color a few times
        styxView.fetchMetaThemeColorTries = KFetchMetaThemeColorTries;

        uiController.tabChanged(styxView)
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView,
        handler: HttpAuthHandler,
        host: String,
        realm: String
    ) {
        AlertDialog.Builder(activity).apply {
            val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_auth_request, null)

            val realmLabel = dialogView.findViewById<TextView>(R.id.auth_request_realm_textview)
            val name = dialogView.findViewById<EditText>(R.id.auth_request_username_edittext)
            val password = dialogView.findViewById<EditText>(R.id.auth_request_password_edittext)

            realmLabel.text = activity.getString(R.string.label_realm, realm)

            setView(dialogView)
            setTitle(R.string.title_sign_in)
            setCancelable(true)
            setPositiveButton(R.string.title_sign_in) { _, _ ->
                val user = name.text.toString()
                val pass = password.text.toString()
                handler.proceed(user.trim(), pass.trim())
                logger.log(TAG, "Attempting HTTP Authentication")
            }
            setNegativeButton(R.string.action_cancel) { _, _ ->
                handler.cancel()
            }
        }.resizeAndShow()
    }

    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        if (view.isShown && styxView.userPreferences.textReflowEnabled) {
            if (isRunning)
                return
            val changeInPercent = abs(100 - 100 / zoomScale * newScale)
            if (changeInPercent > 2.5f && !isRunning) {
                isRunning = view.postDelayed({
                    zoomScale = newScale
                    view.evaluateJavascript(textReflowJs.provideJs()) { isRunning = false }
                }, 100)
            }

        }
    }

    override fun onReceivedSslError(webView: WebView, handler: SslErrorHandler, error: SslError) {
        urlWithSslError = webView.url
        sslState = SslState.Invalid(error)

        when (sslWarningPreferences.recallBehaviorForDomain(webView.url)) {
            SslWarningPreferences.Behavior.PROCEED -> return handler.proceed()
            SslWarningPreferences.Behavior.CANCEL -> return handler.cancel()
            null -> Unit
        }

        val errorCodeMessageCodes = getAllSslErrorMessageCodes(error)

        val stringBuilder = StringBuilder()
        for (messageCode in errorCodeMessageCodes) {
            stringBuilder.append(" - ").append(activity.getString(messageCode)).append('\n')
        }
        val alertMessage = activity.getString(R.string.message_insecure_connection, stringBuilder.toString())

        AlertDialog.Builder(activity).apply {
            val view = LayoutInflater.from(activity).inflate(R.layout.dialog_ssl_warning, null)
            val dontAskAgain = view.findViewById<CheckBox>(R.id.checkBoxDontAskAgain)
            setTitle(activity.getString(R.string.title_warning))
            setMessage(alertMessage)
            setCancelable(true)
            setView(view)
            setOnCancelListener { handler.cancel() }
            setPositiveButton(activity.getString(R.string.action_yes)) { _, _ ->
                if (dontAskAgain.isChecked) {
                    webView.url?.let { sslWarningPreferences.rememberBehaviorForDomain(it, SslWarningPreferences.Behavior.PROCEED) }
                }
                handler.proceed()
            }
            setNegativeButton(activity.getString(R.string.action_no)) { _, _ ->
                if (dontAskAgain.isChecked) {
                    webView.url?.let { sslWarningPreferences.rememberBehaviorForDomain(it, SslWarningPreferences.Behavior.CANCEL) }
                }
                handler.cancel()
            }
        }.resizeAndShow()
    }

    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        AlertDialog.Builder(activity).apply {
            setTitle(activity.getString(R.string.title_form_resubmission))
            setMessage(activity.getString(R.string.message_form_resubmission))
            setCancelable(true)
            setPositiveButton(activity.getString(R.string.action_yes)) { _, _ ->
                resend.sendToTarget()
            }
            setNegativeButton(activity.getString(R.string.action_no)) { _, _ ->
                dontResend.sendToTarget()
            }
        }.resizeAndShow()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        shouldOverrideLoading(view, request.url.toString()) || super.shouldOverrideUrlLoading(view, request)

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
        shouldOverrideLoading(view, url) || super.shouldOverrideUrlLoading(view, url)

    // We use this to prevent opening such dialogs multiple times
    var exAppLaunchDialog: AlertDialog? = null

    private fun shouldOverrideLoading(view: WebView, url: String): Boolean {
        // Check if configured proxy is available
        if (!proxyUtils.isProxyReady(activity)) {
            // User has been notified
            return true
        }

        val headers = styxView.requestHeaders

        if (styxView.isIncognito) {
            // If we are in incognito, immediately load, we don't want the url to leave the app
            return continueLoadingUrl(view, url, headers)
        }
        if (URLUtil.isAboutUrl(url)) {
            // If this is an about page, immediately load, we don't need to leave the app
            return continueLoadingUrl(view, url, headers)
        }

        if (isMailOrIntent(url, view)) {
            // If it was a mailto: link, or an intent, or could be launched elsewhere, do that
            return true
        }

        val intent = intentUtils.intentForUrl(view, url)
        intent?.let {
            // Check if that external app is already known
            val prefKey = activity.getString(R.string.settings_app_prefix) + Uri.parse(url).host
            if (preferences.contains(prefKey)) {
                if (preferences.getBoolean(prefKey,false)) {
                    // Trusted app, just launch it on the stop and abort loading
                    intentUtils.startActivityForIntent(intent)
                    return true
                } else {
                    // User does not want use to use this app
                    return false
                }
            }

            // We first encounter that app ask user if we should use it?
            // We will keep loading even if an external app is available the first time we encounter it.
            (activity as BrowserActivity).mainHandler.postDelayed({
                if (exAppLaunchDialog==null) {
                exAppLaunchDialog = AlertDialog.Builder(activity).setTitle(R.string.dialog_title_third_party_app).setMessage(R.string.dialog_message_third_party_app)
                    .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, id ->
                        // Handle Ok
                        intentUtils.startActivityForIntent(intent)
                        dialog.dismiss()
                        exAppLaunchDialog = null
                        // Remember user choice
                        preferences.edit().putBoolean(prefKey,true).apply()
                    })
                    .setNegativeButton("No", DialogInterface.OnClickListener { dialog, id ->
                        // Handle Cancel
                        dialog.dismiss()
                        exAppLaunchDialog = null
                        // Remember user choice
                        preferences.edit().putBoolean(prefKey,false).apply()
                    })
                    .create()
                exAppLaunchDialog?.show()
                }},1000)
        }

        // If none of the special conditions was met, continue with loading the url
        return continueLoadingUrl(view, url, headers)
    }

    private fun continueLoadingUrl(webView: WebView, url: String, headers: Map<String, String>): Boolean {
        if (!URLUtil.isNetworkUrl(url)
            && !URLUtil.isFileUrl(url)
            && !URLUtil.isAboutUrl(url)
            && !URLUtil.isDataUrl(url)
            && !URLUtil.isJavaScriptUrl(url)) {
            webView.stopLoading()
            return true
        }
        return when {
            headers.isEmpty() -> false
            else -> {
                webView.loadUrl(url, headers)
                true
            }
        }
    }

    private fun isMailOrIntent(url: String, view: WebView): Boolean {
        if (url.startsWith("mailto:")) {
            val mailTo = MailTo.parse(url)
            val i = Utils.newEmailIntent(mailTo.to, mailTo.subject, mailTo.body, mailTo.cc)
            activity.startActivity(i)
            view.reload()
            return true
        } else if (url.startsWith("intent://")) {
            val intent = try {
                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            } catch (ignored: URISyntaxException) {
                null
            }

            if (intent != null) {
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.component = null
                intent.selector = null
                try {
                    activity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    logger.log(TAG, "ActivityNotFoundException")
                }

                return true
            }
        } else if (URLUtil.isFileUrl(url) && !url.isSpecialUrl()) {
            val file = File(url.replace(FILE, ""))

            if (file.exists()) {
                val newMimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(Utils.guessFileExtension(file.toString()))

                val intent = Intent(Intent.ACTION_VIEW)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val contentUri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileprovider", file)
                intent.setDataAndType(contentUri, newMimeType)

                try {
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    println("StyxWebClient: cannot open downloaded file")
                }

            } else {
                activity.snackbar(R.string.message_open_download_fail)
            }
            return true
        }
        return false
    }

    private fun getAllSslErrorMessageCodes(error: SslError): List<Int> {
        val errorCodeMessageCodes = ArrayList<Int>(1)

        if (error.hasError(SslError.SSL_DATE_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_date_invalid)
        }
        if (error.hasError(SslError.SSL_EXPIRED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_expired)
        }
        if (error.hasError(SslError.SSL_IDMISMATCH)) {
            errorCodeMessageCodes.add(R.string.message_certificate_domain_mismatch)
        }
        if (error.hasError(SslError.SSL_NOTYETVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_not_yet_valid)
        }
        if (error.hasError(SslError.SSL_UNTRUSTED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_untrusted)
        }
        if (error.hasError(SslError.SSL_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_invalid)
        }

        return errorCodeMessageCodes
    }

    companion object {

        private const val TAG = "StyxWebClient"

    }
}