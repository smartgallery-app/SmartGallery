package com.smartgallery.app.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartgallery.app.R

class FullScreenPagerAdapter(private val mediaList: List<MediaItem>) :
    RecyclerView.Adapter<FullScreenPagerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imgFullScreenItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_full_screen_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mediaList[position]
        Glide.with(holder.imageView.context)
            .load(Uri.parse(item.uri))
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = mediaList.size
    }
    
