package com.smartgallery.app.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartgallery.app.R

class TrashAdapter(
    private val trashList: List<TrashItem>,
    private val isStealthBox: Boolean = false,
    private val onItemClick: (TrashItem) -> Unit
) : RecyclerView.Adapter<TrashAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumb: ImageView = view.findViewById(R.id.imgTrashThumb)
        val tvTime: TextView = view.findViewById(R.id.tvRemainingTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trash_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = trashList[position]

        // Load Photo
        Glide.with(holder.imgThumb.context)
            .load(Uri.parse(item.uri))
            .into(holder.imgThumb)

        // Photo timeline calculation (No header text)
        if (isStealthBox) {
            // 12-Hour Stealth Recovery Box Timeline
            val hoursLeft = item.getRemainingHours() // e.g. 11h, 5h
            holder.tvTime.text = "${hoursLeft}h"
        } else {
            // 30-Day Main Trash Timeline
            val daysLeft = item.getRemainingDays() // e.g. 29 days, 15 days
            holder.tvTime.text = "${daysLeft} days"
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = trashList.size
}
