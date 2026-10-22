package com.smartgallery.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class MainActivity : AppCompatActivity() {

    private lateinit var mainAdapter: MainAdapter
    private lateinit var albumsAdapter: AlbumsAdapter
    private val mediaList = mutableListOf<MediaItem>()
    private val albumList = mutableListOf<AlbumItem>()
    private val STORAGE_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewMain)
        val tabPhotos = findViewById<TextView>(R.id.tabPhotos)
        val tabAlbums = findViewById<TextView>(R.id.tabAlbums)
        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        val tvMainTitle = findViewById<TextView>(R.id.tvMainTitle)

        recyclerView?.layoutManager = GridLayoutManager(this, 3)

        mainAdapter = MainAdapter(mediaList) { item ->
            val intent = Intent(this, FullScreenActivity::class.java)
            intent.putExtra("media_uri", item.uri)
            startActivity(intent)
        }

        albumsAdapter = AlbumsAdapter(albumList) { album ->
            // Album click
        }

        recyclerView?.adapter = mainAdapter

        tabPhotos?.setOnClickListener {
            tvMainTitle?.text = "Photos"
            tabPhotos.setBackgroundColor(Color.parseColor("#3A3A3C"))
            tabPhotos.setTextColor(Color.WHITE)
            tabAlbums?.setBackgroundColor(Color.TRANSPARENT)
            tabAlbums?.setTextColor(Color.parseColor("#8E8E93"))
            recyclerView?.layoutManager = GridLayoutManager(this, 3)
            recyclerView?.adapter = mainAdapter
        }

        tabAlbums?.setOnClickListener {
            tvMainTitle?.text = "Albums"
            tabAlbums.setBackgroundColor(Color.parseColor("#3A3A3C"))
            tabAlbums.setTextColor(Color.WHITE)
            tabPhotos?.setBackgroundColor(Color.TRANSPARENT)
            tabPhotos?.setTextColor(Color.parseColor("#8E8E93"))
            recyclerView?.layoutManager = GridLayoutManager(this, 2)
            recyclerView?.adapter = albumsAdapter
        }

        btnSettings?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadMediaFiles()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), STORAGE_PERMISSION_CODE)
        }
    }

    private fun loadMediaFiles() {
        mediaList.clear()
        albumList.clear()
        val albumMap = mutableMapOf<String, MutableList<MediaItem>>()

        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

        val cursor = contentResolver.query(uri, projection, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC")
        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val bucketCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val path = c.getString(dataCol) ?: ""
                val dateAdded = c.getLong(dateCol)
                val bucket = c.getString(bucketCol) ?: "Gallery"

                val item = MediaItem(id, path, dateAdded, bucket)
                mediaList.add(item)

                if (!albumMap.containsKey(bucket)) {
                    albumMap[bucket] = mutableListOf()
                }
                albumMap[bucket]?.add(item)
            }
        }

        for ((name, items) in albumMap) {
            if (items.isNotEmpty()) {
                albumList.add(AlbumItem(name, items.size, items[0].uri))
            }
        }

        mainAdapter.notifyDataSetChanged()
        albumsAdapter.notifyDataSetChanged()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMediaFiles()
        } else {
            Toast.makeText(this, "Storage permission required!", Toast.LENGTH_SHORT).show()
        }
    }
}
