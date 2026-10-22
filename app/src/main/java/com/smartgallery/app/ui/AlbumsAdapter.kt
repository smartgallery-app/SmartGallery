package com.smartgallery.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartgallery.app.R

class AlbumsAdapter(
    private val albumList: List<AlbumItem>,
    private val onAlbumClick: (AlbumItem) -> Unit
) : RecyclerView.Adapter<AlbumsAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCover: ImageView = itemView.findViewById(R.id.imgAlbumCover)
        val tvName: TextView = itemView.findViewById(R.id.tvAlbumName)
        val tvCount: TextView = itemView.findViewById(R.id.tvAlbumCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_album_card, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albumList[position]
        holder.tvName.text = album.name
        holder.tvCount.text = "${album.count} items"

        Glide.with(holder.itemView.context)
            .load(album.coverUri)
            .centerCrop()
            .into(holder.imgCover)

        holder.itemView.setOnClickListener {
            onAlbumClick(album)
        }
    }

    override fun getItemCount(): Int = albumList.size
}
