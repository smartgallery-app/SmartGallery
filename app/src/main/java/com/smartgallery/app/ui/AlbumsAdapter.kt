package com.smartgallery.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class AlbumsAdapter(
    private val foldersList: List<String>,
    private val onSectionClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER_FEATURED = 0
        const val TYPE_FOLDERS_GRID = 1
        const val TYPE_HEADER_MORE = 2
        const val TYPE_MORE_CONTAINER = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_HEADER_FEATURED
            1 -> TYPE_FOLDERS_GRID
            2 -> TYPE_HEADER_MORE
            3 -> TYPE_MORE_CONTAINER
            else -> TYPE_MORE_CONTAINER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER_FEATURED, TYPE_HEADER_MORE -> {
                val view = inflater.inflate(R.layout.item_section_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_FOLDERS_GRID -> {
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
                if (position == 0) {
                    holder.title.text = "Albums & Folders"
                } else if (position == 2) {
                    holder.title.text = "Utilities"
                }
            }
            is MediaTypesViewHolder -> {
                val folderText = if (foldersList.isNotEmpty()) foldersList.joinToString(", ") else "Camera, DCIM, WhatsApp"
                
                // Safe check taaki app crash na ho
                try {
                    val tv = holder.item1.findViewById<TextView>(android.R.id.text1)
                    tv?.text = "Folders: $folderText"
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                holder.item1.setOnClickListener { onSectionClick("Folders") }
                holder.item2.setOnClickListener { onSectionClick("Portrait") }
                holder.item3.setOnClickListener { onSectionClick("RAW") }
                holder.item4.setOnClickListener { onSectionClick("Bursts") }
            }
            is MoreViewHolder -> {
                try {
                    val deletedCount = TrashStorage.binList.size
                    holder.deletedCountText?.text = "$deletedCount >"
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                holder.privateAlbum.setOnClickListener { onSectionClick("Private") }
                holder.cleanup.setOnClickListener { onSectionClick("Cleanup suggestions") }
                holder.deleted.setOnClickListener { onSectionClick("Recently deleted") }
            }
        }
    }

    override fun getItemCount(): Int = 4

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
        val deletedCountText: TextView? = try { deleted.findViewById(android.R.id.content) } catch (e: Exception) { null }
    }
}
