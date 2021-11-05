/*
 * Copyright © 2020-2021 Jamal Rothfuchs
 * Copyright © 2020-2021 Stéphane Lenclud
 * Copyright © 2015 Anthony Restaino
 */

package com.jamal2367.styx.list

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jamal2367.styx.R
import com.jamal2367.styx.extensions.inflater

/**
 * A simple [RecyclerView.Adapter] that displays a [List] of [String].
 */
class RecyclerViewStringAdapter<T>(
    private val listItems: List<T>,
    private val convertToString: T.() -> String
) : RecyclerView.Adapter<SimpleStringViewHolder>() {

    var onItemClickListener: ((T) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleStringViewHolder =
        SimpleStringViewHolder(
            parent.context.inflater.inflate(R.layout.simple_list_item, parent, false)
        )

    override fun getItemCount(): Int = listItems.size

    override fun onBindViewHolder(holder: SimpleStringViewHolder, position: Int) {
        val item = listItems[position]
        holder.title.text = item.convertToString()
        holder.itemView.setOnClickListener { onItemClickListener?.invoke(item) }
    }

}

/**
 * A simple [RecyclerView.ViewHolder] that displays a single text item.
 */
class SimpleStringViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    /**
     * The text to display.
     */
    val title: TextView = view.findViewById(R.id.title_text)

}
