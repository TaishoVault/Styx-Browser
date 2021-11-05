/*
 * Copyright © 2020-2021 Jamal Rothfuchs
 * Copyright © 2020-2021 Stéphane Lenclud
 * Copyright © 2015 Anthony Restaino
 */

package com.jamal2367.styx.dialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jamal2367.styx.R
import com.jamal2367.styx.extensions.dimen
import com.jamal2367.styx.extensions.inflater
import com.jamal2367.styx.extensions.resizeAndShow
import com.jamal2367.styx.list.RecyclerViewDialogItemAdapter
import com.jamal2367.styx.list.RecyclerViewStringAdapter
import com.jamal2367.styx.utils.DeviceUtils

object BrowserDialog {

    @JvmStatic
    fun show(
            activity: AppCompatActivity,
            @StringRes title: Int,
            vararg items: DialogItem
    ) = show(activity, activity.getString(title), *items)

    fun showWithIcons(context: Context, title: String?, vararg items: DialogItem) {
        val builder = MaterialAlertDialogBuilder(context)

        val layout = context.inflater.inflate(R.layout.list_dialog, null)

        val titleView = layout.findViewById<TextView>(R.id.dialog_title)
        val recyclerView = layout.findViewById<RecyclerView>(R.id.dialog_list)

        val itemList = items.filter(DialogItem::isConditionMet)

        val adapter = RecyclerViewDialogItemAdapter(itemList)

        if (title?.isNotEmpty() == true) {
            titleView.text = title
        }

        recyclerView.apply {
            this.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            this.adapter = adapter
            setHasFixedSize(true)
        }

        builder.setView(layout)

        val dialog = builder.resizeAndShow()

        adapter.onItemClickListener = { item ->
            item.onClick()
            dialog.dismiss()
        }
    }

    @JvmStatic
    fun show(activity: AppCompatActivity, title: String?, vararg items: DialogItem) {
        val builder = MaterialAlertDialogBuilder(activity)

        val layout = activity.inflater.inflate(R.layout.list_dialog, null)

        val titleView = layout.findViewById<TextView>(R.id.dialog_title)
        val recyclerView = layout.findViewById<RecyclerView>(R.id.dialog_list)

        val itemList = items.filter(DialogItem::isConditionMet)

        val adapter = RecyclerViewStringAdapter(itemList, convertToString = { activity.getString(this.title) })

        if (title?.isNotEmpty() == true) {
            titleView.text = title
        }

        recyclerView.apply {
            this.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            this.adapter = adapter
            setHasFixedSize(true)
        }

        builder.setView(layout)

        val dialog = builder.resizeAndShow()

        adapter.onItemClickListener = { item ->
            item.onClick()
            dialog.dismiss()
        }
    }

    @JvmStatic
    fun showPositiveNegativeDialog(
            activity: AppCompatActivity,
            @StringRes title: Int,
            @StringRes message: Int,
            messageArguments: Array<Any>? = null,
            positiveButton: DialogItem,
            negativeButton: DialogItem,
            onCancel: () -> Unit
    ) {
        val messageValue = if (messageArguments != null) {
            activity.getString(message, *messageArguments)
        } else {
            activity.getString(message)
        }
        MaterialAlertDialogBuilder(activity).apply {
            setTitle(title)
            setMessage(messageValue)
            setOnCancelListener { onCancel() }
            setPositiveButton(positiveButton.title) { _, _ -> positiveButton.onClick() }
            setNegativeButton(negativeButton.title) { _, _ -> negativeButton.onClick() }
        }.resizeAndShow()
    }

    @JvmStatic
    fun showEditText(
            activity: AppCompatActivity,
            @StringRes title: Int,
            @StringRes hint: Int,
            currentText: String?,
            @StringRes action: Int,
            textInputListener: (String) -> Unit
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<EditText>(R.id.dialog_edit_text)

        editText.setHint(hint)
        if (currentText != null) {
            editText.setText(currentText)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(action
            ) { _, _ -> textInputListener(editText.text.toString()) }
            .resizeAndShow()
    }

    @JvmStatic
    fun setDialogSize(context: Context, dialog: Dialog) {
        var maxWidth = context.dimen(R.dimen.dialog_max_size)
        val padding = context.dimen(R.dimen.dialog_padding)
        val screenSize = DeviceUtils.getScreenWidth(context)
        if (maxWidth > screenSize - 2 * padding) {
            maxWidth = screenSize - 2 * padding
        }
        dialog.window?.setLayout(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    /**
     * Show the custom dialog with the custom builder arguments applied.
     */
    fun showCustomDialog(activity: AppCompatActivity, block: MaterialAlertDialogBuilder.(AppCompatActivity) -> Unit) : Dialog {
        MaterialAlertDialogBuilder(activity).apply {
            block(activity)
            return resizeAndShow()
        }
    }

}
