package com.smartgallery.app.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartgallery.app.R

class GalleryAdapter(
    private val mediaList: List<MediaItem>,
    private val isAlbumView: Boolean = false,
    private val onSelectionChanged: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    val selectedItems = mutableSetOf<Int>()
    var isMultiSelectMode = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mediaList[position]

        Glide.with(holder.itemView.context)
            .load(Uri.parse(item.uri))
            .into(holder.imageView)

        // Selected photos par visual effect (dim/scale)
        if (selectedItems.contains(position)) {
            holder.itemView.alpha = 0.5f
            holder.itemView.scaleX = 0.9f
            holder.itemView.scaleY = 0.9f
        } else {
            holder.itemView.alpha = 1.0f
            holder.itemView.scaleX = 1.0f
            holder.itemView.scaleY = 1.0f
        }

        // Tap karne par ya toh zoom khulega ya selection toggle hogi
        holder.itemView.setOnClickListener {
            if (isMultiSelectMode) {
                toggleSelection(position)
            } else {
                val context = holder.itemView.context
                val intent = Intent(context, FullScreenActivity::class.java).apply {
                    putExtra("MEDIA_URI", item.uri)
                    putExtra("MEDIA_ID", item.id)
                    putExtra("MEDIA_FOLDER", item.bucketName)
                    putExtra("MEDIA_DATE", item.dateAdded)
                }
                context.startActivity(intent)
            }
        }

        // Long press karne par Multi-Select Mode chalu ho jayega
        holder.itemView.setOnLongClickListener {
            if (!isMultiSelectMode) {
                isMultiSelectMode = true
            }
            toggleSelection(position)
            true
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
            if (selectedItems.isEmpty()) {
                isMultiSelectMode = false
            }
        } else {
            selectedItems.add(position)
        }
        notifyItemChanged(position)
        onSelectionChanged?.invoke(selectedItems.size)
    }

    override fun getItemCount(): Int = mediaList.size
}
