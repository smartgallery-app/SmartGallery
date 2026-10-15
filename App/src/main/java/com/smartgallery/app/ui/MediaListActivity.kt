package com.smartgallery.app.ui

import android.content.ContentUris
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartgallery.app.R

class MediaListActivity : AppCompatActivity() {

    private val mediaList = mutableListOf<String>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val folderName = intent.getStringExtra("FOLDER_NAME") ?: ""
        supportActionBar?.title = folderName

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        
        adapter = MediaAdapter(this, mediaList) { uri ->
            val intent = Intent(this, FullScreenActivity::class.java)
            intent.putExtra("MEDIA_URI", uri)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        loadMediaForFolder(folderName)
    }

    private fun loadMediaForFolder(folderName: String) {
        mediaList.clear()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(folderName)
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                mediaList.add(uri)
            }
        }
        adapter.notifyDataSetChanged()
    }
}

class MediaAdapter(
    private val context: android.content.Context,
    private val mediaItems: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imgThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val uri = mediaItems[position]
        Glide.with(context)
            .load(uri)
            .centerCrop()
            .into(holder.imageView)

        holder.itemView.setOnClickListener { onClick(uri) }
    }

    override fun getItemCount(): Int = mediaItems.size
}
