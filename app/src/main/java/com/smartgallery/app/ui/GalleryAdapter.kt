package com.smartgallery.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartgallery.app.R

class GalleryAdapter(
    private val context: Context,
    private val mediaList: List<MainActivity.MediaItem>,
    private val onClick: (MainActivity.MediaItem) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val videoIcon: ImageView = view.findViewById(R.id.videoIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaItem = mediaList[position]

        Glide.with(context)
            .load(mediaItem.uriString)
            .centerCrop()
            .into(holder.imageView)

        if (mediaItem.isVideo) {
            holder.videoIcon.visibility = View.VISIBLE
        } else {
            holder.videoIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onClick(mediaItem)
        }
    }

    override fun getItemCount(): Int = mediaList.size
}
