package com.smartgallery.app.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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

        if (item.uri.isNotEmpty()) {
            Glide.with(holder.imageView.context)
                .load(Uri.parse(item.uri))
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(350, 350)
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.app_icon)
        }

        if (isAlbumView) {
            holder.albumNameText.visibility = View.VISIBLE
            holder.albumNameText.text = item.bucketName
        } else {
            holder.albumNameText.visibility = View.GONE
        }

        // Safe Click handling to prevent crash
        holder.itemView.setOnClickListener {
            try {
                if (item.uri.isNotEmpty()) {
                    val context = holder.itemView.context
                    val intent = Intent(context, FullScreenActivity::class.java).apply {
                        putExtra("MEDIA_URI", item.uri)
                        putExtra("MEDIA_ID", item.id)
                        putExtra("MEDIA_DATE", item.dateAdded)
                        putExtra("MEDIA_FOLDER", item.bucketName)
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getItemCount(): Int = mediaList.size
    }
    
