package com.smartgallery.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnPhotos: TextView
    private lateinit var btnAlbums: TextView
    private val PERMISSION_REQUEST_CODE = 100

    private val allPhotos = ArrayList<String>()
    private val allAlbums = ArrayList<String>()
    private var isAlbumView = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            recyclerView = findViewById(R.id.recyclerView)
            recyclerView.layoutManager = GridLayoutManager(this, 4)

            btnPhotos = findViewById(R.id.photosTab) // Make sure ID matches your XML layout
            btnAlbums = findViewById(R.id.albumsTab) // Make sure ID matches your XML layout

            btnPhotos.setOnClickListener {
                isAlbumView = false
                btnPhotos.alpha = 1.0f
                btnAlbums.alpha = 0.6f
                showPhotosGrid()
            }

            btnAlbums.setOnClickListener {
                isAlbumView = true
                btnAlbums.alpha = 1.0f
                btnPhotos.alpha = 0.6f
                showAlbumsGrid()
            }

            checkAndRequestPermissions()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            loadMediaData()
        }
    }

    private fun loadMediaData() {
        allPhotos.clear()
        allAlbums.clear()
        val albumSet = HashSet<String>()

        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                while (it.moveToNext()) {
                    val imagePath = it.getString(columnIndex)
                    if (imagePath != null) {
                        val file = File(imagePath)
                        if (file.exists()) {
                            allPhotos.add(imagePath)
                            file.parentFile?.name?.let { folderName ->
                                albumSet.add(folderName)
                            }
                        }
                    }
                }
            }
            allAlbums.addAll(albumSet)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        showPhotosGrid()
    }

    private fun showPhotosGrid() {
        if (allPhotos.isNotEmpty()) {
            recyclerView.adapter = MediaAdapter(allPhotos) { path ->
                // Open Photo in Full Screen / Full Frame
                val intent = Intent(this, FullScreenActivity::class.java).apply {
                    putExtra("photo_path", path)
                }
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "No photos found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAlbumsGrid() {
        if (allAlbums.isNotEmpty()) {
            recyclerView.adapter = AlbumAdapter(allAlbums) { albumName ->
                Toast.makeText(this, "Album: $albumName", Toast.LENGTH_SHORT).show()
                // Yahan aap specific album ki photos filter karke dikha sakte hain
            }
        } else {
            Toast.makeText(this, "No albums found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                loadMediaData()
            } else {
                Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Adapter for Photos Grid with Click Listener for Full Screen
    class MediaAdapter(private val items: List<String>, private val onClick: (String) -> Unit) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
        class ViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val imageView = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    parent.width / 4,
                    parent.width / 4
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(2, 2, 2, 2)
            }
            return ViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val path = items[position]
            Glide.with(holder.imageView.context)
                .load(File(path))
                .centerCrop()
                .into(holder.imageView)

            holder.itemView.setOnClickListener {
                onClick(path)
            }
        }

        override fun getItemCount() = items.size
    }

    // Adapter for Folders/Albums View
    class AlbumAdapter(private val items: List<String>, private val onClick: (String) -> Unit) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {
        class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val textView = TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    150
                )
                setPadding(30, 40, 30, 40)
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
            }
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val albumName = items[position]
            holder.textView.text = "📁 $albumName"
            holder.itemView.setOnClickListener {
                onClick(albumName)
            }
        }

        override fun getItemCount() = items.size
    }
}
