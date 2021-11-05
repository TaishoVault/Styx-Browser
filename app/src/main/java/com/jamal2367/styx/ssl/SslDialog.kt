/*
 * Copyright © 2020-2021 Jamal Rothfuchs
 * Copyright © 2020-2021 Stéphane Lenclud
 * Copyright © 2015 Anthony Restaino
 */

package com.jamal2367.styx.ssl

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslCertificate
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamal2367.styx.R
import com.jamal2367.styx.extensions.inflater
import com.jamal2367.styx.extensions.resizeAndShow

/**
 * Shows an informative dialog with the provided [SslCertificate] information.
 */
@SuppressLint("CutPasteId")
fun Context.showSslDialog(sslCertificate: SslCertificate, sslState: SslState) {
    val by = sslCertificate.issuedBy
    val to = sslCertificate.issuedTo
    val issueDate = sslCertificate.validNotBeforeDate
    val expireDate = sslCertificate.validNotAfterDate
    val dateFormat = DateFormat.getDateFormat(applicationContext)
    val contentView = inflater.inflate(R.layout.dialog_ssl_info, null, false).apply {
        findViewById<TextView>(R.id.ssl_layout_issue_by).text = by.dName
        findViewById<TextView>(R.id.ssl_layout_issue_to).text = to.dName?.takeIf(String::isNotBlank) ?: to.cName
        findViewById<TextView>(R.id.ssl_layout_issue_date).text = dateFormat.format(issueDate)
        findViewById<TextView>(R.id.ssl_layout_expire_date).text = dateFormat.format(expireDate)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val cert = sslCertificate.x509Certificate
            findViewById<TextView>(R.id.ssl_layout_serial_number).text = cert?.sigAlgName
            findViewById<TextView>(R.id.ssl_layout_algorithm_oid).text = cert?.sigAlgOID
        } else {
            findViewById<TextView>(R.id.ssl_layout_serial_number).visibility = View.GONE
            findViewById<TextView>(R.id.ssl_layout_algorithm_oid).visibility = View.GONE
            findViewById<TextView>(R.id.algorithm).visibility = View.GONE
        }
    }

    val icon = createSslDrawableForState(sslState)

    MaterialAlertDialogBuilder(this)
        .setIcon(icon)
        .setTitle(to.cName)
        .setView(contentView)
        .setPositiveButton(R.string.action_ok, null)
        .resizeAndShow()
}
