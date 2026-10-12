package com.smartgallery.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MediaAdapter(
    private var mediaList: List<MediaItem>,
    private val onItemClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.itemImageView)
        val videoIndicator: ImageView = view.findViewById(R.id.videoIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val item = mediaList[position]
        
        Glide.with(holder.imageView.context)
            .load(item.uri)
            .centerCrop()
            .into(holder.imageView)

        holder.videoIndicator.visibility = if (item.isVideo) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = mediaList.size

    fun updateData(newList: List<MediaItem>) {
        mediaList = newList
        notifyDataSetChanged()
    }
}
