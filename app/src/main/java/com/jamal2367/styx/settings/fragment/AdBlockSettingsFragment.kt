package com.jamal2367.styx.settings.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.addTextChangedListener
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamal2367.styx.R
import com.jamal2367.styx.adblock.BloomFilterAdBlocker
import com.jamal2367.styx.adblock.repository.abp.AbpDao
import com.jamal2367.styx.adblock.repository.abp.AbpEntity
import com.jamal2367.styx.adblock.source.HostsSourceType
import com.jamal2367.styx.adblock.source.selectedHostsSource
import com.jamal2367.styx.adblock.source.toPreferenceIndex
import com.jamal2367.styx.constant.Schemes
import com.jamal2367.styx.di.DiskScheduler
import com.jamal2367.styx.di.MainScheduler
import com.jamal2367.styx.di.injector
//import com.jamal2367.styx.dialog.BrowserDialog
//import com.jamal2367.styx.dialog.DialogItem
import com.jamal2367.styx.extensions.drawable
import com.jamal2367.styx.extensions.snackbar
import com.jamal2367.styx.preference.UserPreferences
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Settings for the ad block mechanic.
 */
class AdBlockSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler
    @Inject @field:DiskScheduler internal lateinit var diskScheduler: Scheduler
    @Inject internal lateinit var bloomFilterAdBlocker: BloomFilterAdBlocker

    private var recentSummaryUpdater: SummaryUpdater? = null
    private val compositeDisposable = CompositeDisposable()
    private var forceRefreshHostsPreference: Preference? = null

    private var abpDao: AbpDao? = null
    private val entitiyPrefs = mutableMapOf<Int, Preference>()

    override fun providePreferencesXmlResource(): Int = R.xml.preference_ad_block

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        injector.inject(this)

        switchPreference(
                preference = SETTINGS_BLOCKMINING,
                isChecked = userPreferences.blockMiningEnabled,
                onCheckChange = { userPreferences.blockMiningEnabled = it }
        )

        switchPreference(
            preference = "cb_block_ads",
            isChecked = userPreferences.adBlockEnabled,
            onCheckChange = { userPreferences.adBlockEnabled = it }
        )

        //clickableDynamicPreference(
        //    preference = "preference_hosts_source",
        //    summary = userPreferences.selectedHostsSource().toSummary(),
        //    onClick = ::showHostsSourceChooser
        //)

        //forceRefreshHostsPreference = clickableDynamicPreference(
        //    preference = "preference_hosts_refresh_force",
        //    isEnabled = isRefreshHostsEnabled(),
        //    onClick = {
        //        bloomFilterAdBlocker.populateAdBlockerFromDataSource(forceRefresh = true)
        //        (activity as AppCompatActivity).snackbar(R.string.block_ad_refresh_hosts)
        //    }
        //)

        if (context != null) {
            abpDao = AbpDao(requireContext())

            // "new list" button
            val pref = Preference(context)
            pref.title = getString(R.string.ad_block_create_blocklist)
            pref.icon = requireContext().drawable(R.drawable.ic_add_oval)
            pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setNegativeButton(getString(R.string.ad_block_from_file)) { _,_ -> showBlockist(AbpEntity(url = getString(R.string.ad_block_file))) }
                    .setNeutralButton(getString(R.string.ad_block_from_address)) { _,_ -> showBlockist(AbpEntity(url = "")) }
                    .setPositiveButton(getString(R.string.action_cancel), null)
                    .setTitle(getString(R.string.ad_block_create_blocklist))
                    .create()
                dialog.show()
                true
            }
            this.preferenceScreen.addPreference(pref)

            for (entity in abpDao!!.getAll()) {
                val entityPref = Preference(context)
                entityPref.title = entity.title
                entityPref.icon = requireContext().drawable(R.drawable.ic_import_export_oval)
                entityPref.summary = getString(R.string.ad_block_last_update) + " ${entity.lastUpdate}"
                entityPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    showBlockist(entity)
                    true
                }
                entitiyPrefs[entity.entityId] = entityPref
                this.preferenceScreen.addPreference(entitiyPrefs[entity.entityId])
            }
        }
    }

    private fun showBlockist(entity: AbpEntity) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        var dialog: AlertDialog? = null
        builder.setTitle(getString(R.string.ad_block_edit_blocklist))
        val ll = LinearLayout(context)
        ll.orientation = LinearLayout.VERTICAL

        val name = EditText(context)
        name.inputType = InputType.TYPE_CLASS_TEXT
        name.setText(entity.title)
        name.hint = getString(R.string.ad_block_name)
        ll.addView(name)
        // some listener that checks for §§? or do when clicking ok?

        when {
            entity.url.startsWith(Schemes.Styx) -> {
                val text = TextView(context)
                text.text = getString(R.string.ad_block_internal_list)
                ll.addView(text)
            }
            entity.url.startsWith(getString(R.string.ad_block_file)) -> {
                val updateButton = Button(context)
                updateButton.text = getString(R.string.ad_block_update_list)
                updateButton.setOnClickListener {
                }
                ll.addView(updateButton)
            }
            entity.url.toHttpUrlOrNull() != null || entity.url == "" -> {
                val url = EditText(context)
                url.inputType = InputType.TYPE_TEXT_VARIATION_URI
                url.setText(entity.url)
                url.hint = getString(R.string.ad_block_address)
                url.addTextChangedListener {
                    entity.homePage = it.toString()
                    // disable ok button if url not valid
                    dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = it.toString().toHttpUrlOrNull() != null
                    dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.text = if (it.toString().toHttpUrlOrNull() != null)
                        getString(R.string.action_ok)
                    else
                        getString(R.string.ad_block_invalid_url)
                }
                ll.addView(url)
            }
        }

        val enabled = SwitchCompat(requireContext())
        enabled.text = getString(R.string.ad_block_enable)
        enabled.isChecked = entity.enabled
        ll.addView(enabled)

        // only show if not creating a new entity
        if (entity.entityId != 0) {
            val delete = Button(context) // looks ugly, but works
            delete.text = getString(R.string.ad_block_delete_list)
            delete.setOnClickListener {
                // confirm delete!
                val really = MaterialAlertDialogBuilder(requireContext())
                    .setNegativeButton(getString(R.string.no), null)
                    .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                        abpDao?.delete(entity)
                        dialog?.dismiss()
                        preferenceScreen.removePreference(entitiyPrefs[entity.entityId]) // working?
                    }
                    .setTitle(getString(R.string.action_delete) + "?")
                    .create()
                really.show()
            }
            ll.addView(delete)
        }

        // arbitrary numbers that look ok on my phone -> ok for other phones?
        //  what unit is this? dp? px?
        ll.setPadding(30,10,30,10)
        builder.setView(ll)
        builder.setNegativeButton(getString(R.string.action_cancel), null)
        builder.setPositiveButton(getString(R.string.action_ok)) { _,_ ->
            if (name.text.toString().contains("§§") || entity.url.contains("§§"))
                context?.let { AlertDialog.Builder(it) } // how to not accept click on ok? menu dialog should remain open
            // so better disable the ok button as long as §§ is found? but needs some kind of text change listener
            // -> https://stackoverflow.com/questions/2620444/how-to-prevent-a-dialog-from-closing-when-a-button-is-clicked
            //  or add another addTextChangedListener for the name text?
            //  but then take care that always both texts are ok!

            entity.enabled = enabled.isChecked

            entity.title = name.text.toString()
            val newId = abpDao?.update(entity) // new id if new entity was added
            // download updates immediately? should do, but i need the correct id in case a new list was added

            // maybe only update if title changed? depends how i get it working...
            if (newId != null && entitiyPrefs[newId] == null) { // not in entityPrefs if new
                val pref = Preference(context)
                entity.entityId = newId
                pref.title = entity.title
                pref.summary = getString(R.string.ad_block_edit_blocklist) + "${entity.lastUpdate}"
                pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    showBlockist(entity)
                    true
                }
                entitiyPrefs[newId] = pref
                preferenceScreen.addPreference(entitiyPrefs[newId])
            }

            // is this enough to update title?
            entitiyPrefs[entity.entityId]?.title = entity.title

        }
        dialog = builder.create()
        dialog.show()
        if (entity.url == "") { // editText field is not checked on start
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).text = getString(R.string.ad_block_invalid_url)
        }

    }

    private fun updateRefreshHostsEnabledStatus() {
        forceRefreshHostsPreference?.isEnabled = isRefreshHostsEnabled()
    }

    private fun isRefreshHostsEnabled() = userPreferences.selectedHostsSource() is HostsSourceType.Remote

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    private fun HostsSourceType.toSummary(): String = when (this) {
        HostsSourceType.Default -> getString(R.string.block_source_default)
        is HostsSourceType.Local -> getString(R.string.block_source_local_description, file.path)
        is HostsSourceType.Remote -> getString(R.string.block_source_remote_description, httpUrl)
    }

    //private fun showHostsSourceChooser(summaryUpdater: SummaryUpdater) {
    //    BrowserDialog.showListChoices(
    //            activity as AppCompatActivity,
    //        R.string.block_ad_source,
    //        DialogItem(
    //            title = R.string.block_source_default,
    //            isConditionMet = userPreferences.selectedHostsSource() == HostsSourceType.Default,
    //            onClick = {
    //                userPreferences.hostsSource = HostsSourceType.Default.toPreferenceIndex()
    //                summaryUpdater.updateSummary(userPreferences.selectedHostsSource().toSummary())
    //                updateForNewHostsSource()
    //            }
    //        ),
    //        DialogItem(
    //            title = R.string.block_source_local,
    //            isConditionMet = userPreferences.selectedHostsSource() is HostsSourceType.Local,
    //            onClick = {
    //                showFileChooser(summaryUpdater)
    //            }
    //        ),
    //        DialogItem(
    //            title = R.string.block_source_remote,
    //            isConditionMet = userPreferences.selectedHostsSource() is HostsSourceType.Remote,
    //            onClick = {
    //                showUrlChooser(summaryUpdater)
    //            }
    //        )
    //    )
    //}

    //@Suppress("DEPRECATION")
    //private fun showFileChooser(summaryUpdater: SummaryUpdater) {
    //    this.recentSummaryUpdater = summaryUpdater
    //    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    //        addCategory(Intent.CATEGORY_OPENABLE)
    //        type = TEXT_MIME_TYPE
    //    }

    //    startActivityForResult(intent, FILE_REQUEST_CODE)
    //}

    //private fun showUrlChooser(summaryUpdater: SummaryUpdater) {
    //    BrowserDialog.showEditText(
    //            activity as AppCompatActivity,
    //        title = R.string.block_source_remote,
    //        hint = R.string.hint_url,
    //        currentText = userPreferences.hostsRemoteFile,
    //        action = R.string.action_ok,
    //        textInputListener = {
    //            val url = it.toHttpUrlOrNull()
    //                ?: return@showEditText run { (activity as AppCompatActivity).snackbar(R.string.problem_download) }
    //            userPreferences.hostsSource = HostsSourceType.Remote(url).toPreferenceIndex()
    //            userPreferences.hostsRemoteFile = it
    //            summaryUpdater.updateSummary(it)
    //            updateForNewHostsSource()
    //        }
    //    )
    //}

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                data?.data?.also { uri ->
                    compositeDisposable += readTextFromUri(uri)
                        .subscribeOn(diskScheduler)
                        .observeOn(mainScheduler)
                        .subscribeBy(
                            onComplete = { (activity as AppCompatActivity).snackbar(R.string.action_message_canceled) },
                            onSuccess = { file ->
                                userPreferences.hostsSource = HostsSourceType.Local(file).toPreferenceIndex()
                                userPreferences.hostsLocalFile = file.path
                                recentSummaryUpdater?.updateSummary(userPreferences.selectedHostsSource().toSummary())
                                updateForNewHostsSource()
                            }
                        )
                }
            } else {
                (activity as AppCompatActivity).snackbar(R.string.action_message_canceled)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun updateForNewHostsSource() {
        bloomFilterAdBlocker.populateAdBlockerFromDataSource(forceRefresh = true)
        updateRefreshHostsEnabledStatus()
    }

    private fun readTextFromUri(uri: Uri): Maybe<File> = Maybe.create {
        val externalFilesDir = activity?.getExternalFilesDir("")
            ?: return@create it.onComplete()
        val inputStream = activity?.contentResolver?.openInputStream(uri)
            ?: return@create it.onComplete()

        try {
            val outputFile = File(externalFilesDir, AD_HOSTS_FILE)

            val input = inputStream.source()
            val output = outputFile.sink().buffer()
            output.writeAll(input)
            return@create it.onSuccess(outputFile)
        } catch (exception: IOException) {
            return@create it.onComplete()
        }
    }

    companion object {
        private const val FILE_REQUEST_CODE = 100
        private const val AD_HOSTS_FILE = "local_hosts.txt"
        //private const val TEXT_MIME_TYPE = "text/*"
        private const val SETTINGS_BLOCKMINING = "block_mining_sites"
    }
}
