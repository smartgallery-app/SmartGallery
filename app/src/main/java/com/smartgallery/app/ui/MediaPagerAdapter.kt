package com.smartgallery.app.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.smartgallery.app.R

class MediaPagerAdapter(private val mediaList: List<MediaItem>) :
    RecyclerView.Adapter<MediaPagerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoView: PhotoView = view.findViewById(R.id.itemPhotoView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_full_screen_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mediaList[position]
        Glide.with(holder.itemView.context)
            .load(Uri.parse(item.uri))
            .into(holder.photoView)
    }

    override fun getItemCount(): Int = mediaList.size
    }
    
