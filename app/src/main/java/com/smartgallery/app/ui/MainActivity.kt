package com.smartgallery.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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

        val btnNavTrash = findViewById<RelativeLayout>(R.id.btnNavTrash)
        val tvMoreHeader = findViewById<TextView>(R.id.tvMoreHeader)
        val cardMoreSection = findViewById<CardView>(R.id.cardMoreSection)

        recyclerView?.layoutManager = GridLayoutManager(this, 3)

        mainAdapter = MainAdapter(mediaList) { item ->
            // FullScreenViewActivity handled safely
        }

        albumsAdapter = AlbumsAdapter(albumList) { album -> }
        recyclerView?.adapter = mainAdapter

        tabPhotos?.setOnClickListener {
            tvMainTitle?.text = "Photos"
            tabPhotos.setBackgroundColor(Color.parseColor("#3A3A3C"))
            tabPhotos.setTextColor(Color.WHITE)
            tabAlbums?.setBackgroundColor(Color.TRANSPARENT)
            tabAlbums?.setTextColor(Color.parseColor("#8E8E93"))

            tvMoreHeader?.visibility = View.GONE
            cardMoreSection?.visibility = View.GONE

            recyclerView?.layoutManager = GridLayoutManager(this, 3)
            recyclerView?.adapter = mainAdapter
        }

        tabAlbums?.setOnClickListener {
            tvMainTitle?.text = "Albums"
            tabAlbums.setBackgroundColor(Color.parseColor("#3A3A3C"))
            tabAlbums.setTextColor(Color.WHITE)
            tabPhotos?.setBackgroundColor(Color.TRANSPARENT)
            tabPhotos?.setTextColor(Color.parseColor("#8E8E93"))

            tvMoreHeader?.visibility = View.VISIBLE
            cardMoreSection?.visibility = View.VISIBLE

            recyclerView?.layoutManager = GridLayoutManager(this, 2)
            recyclerView?.adapter = albumsAdapter
        }

        btnSettings?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnNavTrash?.setOnClickListener {
            startActivity(Intent(this, TrashActivity::class.java))
        }

        // --- 24/7 DUAL-ENGINE BACKGROUND SERVICE START ---
        val serviceIntent = Intent(this, BackgroundScanService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        MediaBackupScanWorker.scheduleBackupEngine(applicationContext)

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), STORAGE_PERMISSION_CODE)
        } else {
            loadMediaFiles()
        }
    }

    private fun loadMediaFiles() {
        mediaList.clear()
        albumList.clear()
        val albumMap = mutableMapOf<String, MutableList<MediaItem>>()

        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val dateAdded = c.getLong(dateCol)
                val bucket = "Camera"

                val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()).toString()
                val item = MediaItem(id, contentUri, dateAdded, bucket)
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
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            loadMediaFiles()
        } else {
            loadMediaFiles()
        }
    }
}
