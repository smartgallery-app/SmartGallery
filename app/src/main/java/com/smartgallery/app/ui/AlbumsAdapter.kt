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

data class AlbumModel(
    val title: String,
    val count: Int,
    val coverUri: String? = null
)

class AlbumsAdapter(
    private val allMedia: List<MediaItem>,
    private val onAlbumClick: (String) -> Unit
) : RecyclerView.Adapter<AlbumsAdapter.AlbumViewHolder>() {

    private val albumList = ArrayList<AlbumModel>()

    init {
        prepareAlbums()
    }

    private fun prepareAlbums() {
        albumList.clear()
        
        // Dynamic Folder Grouping
        val grouped = allMedia.groupBy { it.bucketName }
        
        // 1. Pin Section items
        val recentCover = allMedia.firstOrNull()?.uri
        albumList.add(AlbumModel("Recent", allMedia.size, recentCover))

        for ((folder, items) in grouped) {
            albumList.add(AlbumModel(folder, items.size, items.firstOrNull()?.uri))
        }

        // Dummy categories to match OnePlus layout
        if (grouped["Camera"] == null) albumList.add(AlbumModel("Camera", 0, recentCover))
        if (grouped["Videos"] == null) albumList.add(AlbumModel("Videos", 0, recentCover))
        if (grouped["Screenshots"] == null) albumList.add(AlbumModel("Screenshots", 0, recentCover))
    }

    class AlbumViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val coverImage: ImageView = view.findViewById(R.id.albumCoverImage)
        val title: TextView = view.findViewById(R.id.albumTitle)
        val count: TextView = view.findViewById(R.id.albumCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_album_card, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albumList[position]
        holder.title.text = album.title
        holder.count.text = album.count.toString()

        if (!album.coverUri.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(Uri.parse(album.coverUri))
                .into(holder.coverImage)
        } else {
            holder.coverImage.setBackgroundColor(0xFF222222.toInt())
        }

        holder.itemView.setOnClickListener {
            onAlbumClick(album.title)
        }
    }

    override fun getItemCount(): Int = albumList.size
}
