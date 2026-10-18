package com.smartgallery.app.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class GalleryAdapter(private val mediaList: List<MediaItem>, private val isAlbumView: Boolean) :
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val albumNameText: TextView = view.findViewById(R.id.albumNameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mediaList[position]
        holder.imageView.setImageURI(Uri.parse(item.uri))

        if (isAlbumView) {
            holder.albumNameText.visibility = View.VISIBLE
            holder.albumNameText.text = item.bucketName
        } else {
            holder.albumNameText.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, FullScreenActivity::class.java).apply {
                putExtra("MEDIA_URI", item.uri)
                putExtra("MEDIA_DATE", item.dateAdded.toString())
                putExtra("MEDIA_FOLDER", item.bucketName)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = mediaList.size
    }
    
