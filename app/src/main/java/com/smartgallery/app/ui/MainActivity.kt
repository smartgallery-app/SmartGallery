package com.smartgallery.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SmartGalleryMain"
    }

    private lateinit var mainAdapter: MainAdapter
    private lateinit var albumsAdapter: AlbumsAdapter
    private val mediaList = mutableListOf<MediaItem>()
    private val albumList = mutableListOf<AlbumItem>()
    private val STORAGE_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "onCreate started safely.")
            setContentView(R.layout.activity_main)

            val recyclerViewPhotos = findViewById<RecyclerView>(R.id.recyclerViewPhotos)
            val scrollViewAlbums = findViewById<ScrollView>(R.id.scrollViewAlbums)
            val recyclerViewAlbumsGrid = findViewById<RecyclerView>(R.id.recyclerViewAlbumsGrid)

            val tabPhotos = findViewById<TextView>(R.id.tabPhotos)
            val tabAlbums = findViewById<TextView>(R.id.tabAlbums)
            val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
            val tvMainTitle = findViewById<TextView>(R.id.tvMainTitle)
            val btnNavFilter = findViewById<androidx.cardview.widget.CardView>(R.id.btnNavFilter)
            val btnNavTrash = findViewById<RelativeLayout>(R.id.btnNavTrash)

            recyclerViewPhotos?.layoutManager = GridLayoutManager(this, 3)
            recyclerViewAlbumsGrid?.layoutManager = GridLayoutManager(this, 2)

            // Photo click listener
            mainAdapter = MainAdapter(mediaList) { item ->
                try {
                    val intent = Intent(this, FullScreenViewActivity::class.java).apply {
                        putExtra("media_uri", item.uri)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening FullScreenViewActivity: ${e.message}")
                }
            }

            // Album folder click listener
            albumsAdapter = AlbumsAdapter(albumList) { album ->
                try {
                    val intent = Intent(this, AlbumDetailActivity::class.java).apply {
                        putExtra("album_name", album.name)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening AlbumDetailActivity: ${e.message}")
                }
            }

            recyclerViewPhotos?.adapter = mainAdapter
            recyclerViewAlbumsGrid?.adapter = albumsAdapter

            // Tab Switching Logic
            tabPhotos?.setOnClickListener {
                tvMainTitle?.text = "Photos"
                tabPhotos.setBackgroundColor(Color.parseColor("#3A3A3C"))
                tabPhotos.setTextColor(Color.WHITE)
                tabAlbums?.setBackgroundColor(Color.TRANSPARENT)
                tabAlbums?.setTextColor(Color.parseColor("#8E8E93"))

                recyclerViewPhotos?.visibility = View.VISIBLE
                scrollViewAlbums?.visibility = View.GONE
            }

            tabAlbums?.setOnClickListener {
                tvMainTitle?.text = "Albums"
                tabAlbums.setBackgroundColor(Color.parseColor("#3A3A3C"))
                tabAlbums.setTextColor(Color.WHITE)
                tabPhotos?.setBackgroundColor(Color.TRANSPARENT)
                tabPhotos?.setTextColor(Color.parseColor("#8E8E93"))

                recyclerViewPhotos?.visibility = View.GONE
                scrollViewAlbums?.visibility = View.VISIBLE
            }

            // Filter Popup Menu (Realme style popup when clicking filter button)
            btnNavFilter?.setOnClickListener { view ->
                val popup = PopupMenu(this, view)
                popup.menu.add(0, 1, 0, "All")
                popup.menu.add(0, 2, 1, "Photo")
                popup.menu.add(0, 3, 2, "Videos")
                popup.menu.add(0, 4, 3, "Motion Photo")
                popup.menu.add(0, 5, 4, "Screenshots & screen recordings")
                popup.menu.add(0, 6, 5, "Favourites")
                popup.menu.add(0, 7, 6, "Edited")
                popup.menu.add(0, 8, 7, "Hide customised albums")

                popup.setOnMenuItemClickListener { item ->
                    // Handle filter selection here if needed
                    true
                }
                popup.show()
            }

            btnSettings?.setOnClickListener {
                try {
                    startActivity(Intent(this, SettingsActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening SettingsActivity: ${e.message}")
                }
            }

            btnNavTrash?.setOnClickListener {
                try {
                    startActivity(Intent(this, TrashActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening TrashActivity: ${e.message}")
                }
            }

            checkAndRequestPermissions()

            // --- 10 MINUTES DELAYED START, THEN 24/7 BACKGROUND ENGINE ---
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val serviceIntent = Intent(this, BackgroundScanService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    MediaBackupScanWorker.scheduleBackupEngine(applicationContext)
                    Log.d(TAG, "24/7 Background service successfully triggered after 10 minutes.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start background service after delay: ${e.message}")
                }
            }, 10 * 60 * 1000L)

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in MainActivity onCreate: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun checkAndRequestPermissions() {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAndRequestPermissions: ${e.message}")
        }
    }

    private fun loadMediaFiles() {
        try {
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
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val typeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val filePath = c.getString(dataCol)
                    val mediaType = c.getInt(typeCol)
                    val dateAdded = c.getLong(dateCol)

                    val contentUri = if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                        Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                    } else {
                        Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    }.toString()

                    val bucket = filePath?.let { path ->
                        val file = File(path)
                        file.parentFile?.name ?: "Camera"
                    } ?: "Camera"

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
        } catch (e: Exception) {
            Log.e(TAG, "Error loading media files: ${e.message}", e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            loadMediaFiles()
        }
    }
}
