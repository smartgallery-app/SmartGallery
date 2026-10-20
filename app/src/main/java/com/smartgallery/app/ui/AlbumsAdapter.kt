package com.smartgallery.app.ui

import android.app.AlertDialog
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartgallery.app.R

data class AlbumModel(
    val title: String,
    val count: Int,
    val coverUri: String? = null,
    val isUtility: Boolean = false
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
        val firstCover = allMedia.firstOrNull()?.uri
        albumList.add(AlbumModel("Recent", allMedia.size, firstCover))

        val grouped = allMedia.groupBy { it.bucketName }
        for ((folderName, items) in grouped) {
            albumList.add(AlbumModel(folderName, items.size, items.firstOrNull()?.uri))
        }

        albumList.add(AlbumModel("Private", 0, isUtility = true))
        albumList.add(AlbumModel("Cleanup suggestions", 0, isUtility = true))
        albumList.add(AlbumModel("Recently deleted", TrashStorage.binList.size, TrashStorage.binList.firstOrNull()?.uri, isUtility = true))
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
        holder.count.text = if (album.title == "Private" || album.title == "Cleanup suggestions") ">" else "${album.count} items"

        if (!album.coverUri.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(Uri.parse(album.coverUri))
                .into(holder.coverImage)
        } else {
            holder.coverImage.setBackgroundColor(0xFF222222.toInt())
        }

        holder.itemView.setOnClickListener {
            if (album.title == "Private") {
                showPinDialog(holder.itemView.context) {
                    onAlbumClick(album.title)
                }
            } else {
                onAlbumClick(album.title)
            }
        }
    }

    private fun showPinDialog(context: android.content.Context, onSuccess: () -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_pin_lock, null)
        val etPin = dialogView.findViewById<EditText>(R.id.etPin)
        val btnUnlock = dialogView.findViewById<Button>(R.id.btnUnlock)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnUnlock.setOnClickListener {
            val pin = etPin.text.toString().trim()
            if (pin == "1234") { // Default PIN set as 1234
                dialog.dismiss()
                onSuccess()
            } else {
                Toast.makeText(context, "Incorrect PIN!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun getItemCount(): Int = albumList.size
}
