package com.example.dwnas.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dwnas.R
import com.example.dwnas.database.ListItemLink
import com.example.dwnas.databinding.ListItemBinding


class ItemLinkAdapter(private val listener: Listener): ListAdapter<ListItemLink, ItemLinkAdapter.MyHolder>(
    Comparator()
) {
    class MyHolder(
        view: View,
        listener: Listener
    ) : RecyclerView.ViewHolder(view) {
        private val b = ListItemBinding.bind(view)
        private var item1: ListItemLink? = null

        init {
            b.bCopyBuff.setOnClickListener {
                item1?.let { it1 -> listener.onClickSave(it1) }
            }
            b.bDelObj.setOnClickListener {
                item1?.let { it1 -> listener.onClickDelete(it1) }
            }
        }

        fun bind(item: ListItemLink) = with(b) {
            item1 = item
            try {
                tvLink.text = item1!!.link
                tvName.text = item1!!.name
            } catch (e: SecurityException) {
                Log.d("My error", e.message.toString())
            }
        }
    }

    class Comparator: DiffUtil.ItemCallback<ListItemLink>(){
        override fun areContentsTheSame(oldItem: ListItemLink, newItem: ListItemLink): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: ListItemLink, newItem: ListItemLink): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return MyHolder(view, listener)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.bind(getItem(position))
    }

    interface Listener{
        fun onClickSave(link: ListItemLink)
        fun onClickDelete(link: ListItemLink)
    }
}