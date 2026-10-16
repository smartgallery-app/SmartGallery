package com.smartgallery.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartgallery.app.R

class MediaAdapter(
    private val mediaList: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewMedia: ImageView = itemView.findViewById(R.id.imageViewMedia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaPath = mediaList[position]
        Glide.with(holder.itemView.context)
            .load(mediaPath)
            .centerCrop()
            .into(holder.imageViewMedia)

        holder.itemView.setOnClickListener {
            onItemClick(mediaPath)
        }
    }

    override fun getItemCount(): Int = mediaList.size
}
