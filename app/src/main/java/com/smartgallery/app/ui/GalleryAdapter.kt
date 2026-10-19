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
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_GRID = 1
        private const val TYPE_LIST = 2
    }

    override fun getItemViewType(position: Int): Int {
        val item = mediaList[position]
        return if (item.id < 0L && item.uri.isEmpty()) {
            TYPE_LIST
        } else {
            TYPE_GRID
        }
    }

    class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val albumNameText: TextView = view.findViewById(R.id.albumNameText)
    }

    class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val listIcon: TextView = view.findViewById(R.id.listIcon)
        val listTitle: TextView = view.findViewById(R.id.listTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_LIST) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_album_list, parent, false)
            ListViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_media, parent, false)
            GridViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = mediaList[position]

        if (holder is ListViewHolder) {
            val parts = item.bucketName.split(" ", limit = 2)
            if (parts.size == 2) {
                holder.listIcon.text = parts[0]
                holder.listTitle.text = parts[1]
            } else {
                holder.listIcon.text = "📁"
                holder.listTitle.text = item.bucketName
            }
        } else if (holder is GridViewHolder) {
            if (item.uri.isNotEmpty()) {
                Glide.with(holder.imageView.context)
                    .load(Uri.parse(item.uri))
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(350, 350)
                    .into(holder.imageView)
            } else {
                holder.imageView.setImageDrawable(null)
            }

            if (isAlbumView) {
                holder.albumNameText.visibility = View.VISIBLE
                holder.albumNameText.text = item.bucketName
            } else {
                holder.albumNameText.visibility = View.GONE
            }

            // Safe Photo Click - Crash Free Intent
            holder.itemView.setOnClickListener {
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
            }
        }
    }

    override fun getItemCount(): Int = mediaList.size
    }
    
