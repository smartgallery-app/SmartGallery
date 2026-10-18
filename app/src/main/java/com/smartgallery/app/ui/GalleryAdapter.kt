package com.smartgallery.app.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class GalleryAdapter(private val mediaList: List<MediaItem>) :
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mediaList[position]
        
        // Image set karne ke liye direct URI bind
        holder.imageView.setImageURI(Uri.parse(item.uri))

        // Single Click Par Full Screen View
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, FullScreenActivity::class.java).apply {
                putExtra("MEDIA_URI", item.uri)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = mediaList.size
    }
    
