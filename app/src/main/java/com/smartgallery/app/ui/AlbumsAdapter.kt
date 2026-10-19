package com.smartgallery.app.ui

content_copy
package com.smartgallery.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class AlbumsAdapter(
    private val onSectionClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const TYPE_HEADER = 0
        const TYPE_MEDIA_TYPES_CONTAINER = 1
        const TYPE_MORE_CONTAINER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0, 2 -> TYPE_HEADER
            1 -> TYPE_MEDIA_TYPES_CONTAINER
            3 -> TYPE_MORE_CONTAINER
            else -> TYPE_MEDIA_TYPES_CONTAINER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_section_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_MEDIA_TYPES_CONTAINER -> {
                val view = inflater.inflate(R.layout.item_media_types_card, parent, false)
                MediaTypesViewHolder(view)
            }
            TYPE_MORE_CONTAINER -> {
                val view = inflater.inflate(R.layout.item_more_card, parent, false)
                MoreViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_media_types_card, parent, false)
                MediaTypesViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                if (position == 0) holder.title.text = "Featured"
                else if (position == 2) holder.title.text = "Media types"
            }
            is MediaTypesViewHolder -> {
                holder.item1.setOnClickListener { onSectionClick("Motion Photo") }
                holder.item2.setOnClickListener { onSectionClick("Portrait") }
                holder.item3.setOnClickListener { onSectionClick("RAW") }
                holder.item4.setOnClickListener { onSectionClick("Bursts") }
            }
            is MoreViewHolder -> {
                holder.privateAlbum.setOnClickListener { onSectionClick("Private") }
                holder.cleanup.setOnClickListener { onSectionClick("Cleanup suggestions") }
                holder.deleted.setOnClickListener { onSectionClick("Recently deleted") }
            }
        }
    }

    override fun getItemCount(): Int = 4 // Header, Media Types, Header, More

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.sectionTitleText)
    }

    class MediaTypesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val item1: View = view.findViewById(R.id.layoutMotionPhoto)
        val item2: View = view.findViewById(R.id.layoutPortrait)
        val item3: View = view.findViewById(R.id.layoutRaw)
        val item4: View = view.findViewById(R.id.layoutBursts)
    }

    class MoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val privateAlbum: View = view.findViewById(R.id.layoutPrivate)
        val cleanup: View = view.findViewById(R.id.layoutCleanup)
        val deleted: View = view.findViewById(R.id.layoutDeleted)
    }
}
