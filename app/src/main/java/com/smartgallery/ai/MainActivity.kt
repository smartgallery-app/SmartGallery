package com.smartgallery.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smartgallery.app.R

data class MediaItem(
    val id: Long,
    val uri: String,
    val bucketName: String
)

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101
    private val allMediaList = mutableListOf<MediaItem>()
    private val currentDisplayList = mutableListOf<MediaItem>()
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var fullScreenImageView: ImageView

    private var isFolderView = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        fullScreenImageView = findViewById(R.id.fullScreenImageView)

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        galleryAdapter = GalleryAdapter(this, currentDisplayList) { item ->
            onItemClicked(item)
        }
        recyclerView.adapter = galleryAdapter

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            loadAllMedia()
        }
    }

    private fun loadAllMedia() {
        allMediaList.clear()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val bucketName = c.getString(bucketColumn) ?: "Internal"
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                allMediaList.add(MediaItem(id, uri, bucketName))
            }
        }

        showFolderGrid()
    }

    private fun showFolderGrid() {
        isFolderView = true
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        fullScreenImageView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        val distinctFolders = allMediaList.distinctBy { it.bucketName }
        currentDisplayList.clear()
        currentDisplayList.addAll(distinctFolders)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun showPhotosForFolder(bucketName: String) {
        isFolderView = false
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        
        val folderPhotos = allMediaList.filter { it.bucketName == bucketName }
        currentDisplayList.clear()
        currentDisplayList.addAll(folderPhotos)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun onItemClicked(item: MediaItem) {
        if (isFolderView) {
            showPhotosForFolder(item.bucketName)
        } else {
            recyclerView.visibility = View.GONE
            fullScreenImageView.visibility = View.VISIBLE
            Glide.with(this).load(item.uri).into(fullScreenImageView)
        }
    }

    override fun onBackPressed() {
        if (fullScreenImageView.visibility == View.VISIBLE) {
            fullScreenImageView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else if (!isFolderView) {
            showFolderGrid()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            loadAllMedia()
        }
    }
}

class GalleryAdapter(
    private val context: Context,
    private val items: List<MediaItem>,
    private val onClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgThumbnail: ImageView = itemView.findViewById(R.id.imgThumbnail)
        val txtFolderName: TextView = itemView.findViewById(R.id.txtFolderName)
        val txtItemCount: TextView = itemView.findViewById(R.id.txtItemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtFolderName.text = item.bucketName
        holder.txtItemCount.text = ""

        Glide.with(context)
            .load(item.uri)
            .centerCrop()
            .into(holder.imgThumbnail)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
