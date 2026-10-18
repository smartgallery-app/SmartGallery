package com.smartgallery.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartgallery.app.R

data class MediaItem(
    val id: Long,
    val uri: String,
    val dateAdded: Long,
    val bucketName: String
)

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private val allMediaList = ArrayList<MediaItem>()
    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            recyclerView = findViewById(R.id.recyclerView)
            bottomNav = findViewById(R.id.bottomNavigation)
            recyclerView.layoutManager = GridLayoutManager(this, 3)

            bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_photos -> {
                        loadPhotosView()
                        true
                    }
                    R.id.nav_albums -> {
                        loadAlbumsView()
                        true
                    }
                    R.id.nav_bin -> {
                        loadBinView()
                        true
                    }
                    else -> false
                }
            }

            checkAndRequestPermissions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAndRequestPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        } else {
            loadDeviceMedia()
        }
    }

    private fun loadDeviceMedia() {
        allMediaList.clear()
        val uniqueIds = HashSet<Long>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        try {
            contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val dateAdded = cursor.getLong(dateColumn)
                    val bucketName = cursor.getString(bucketColumn) ?: "Internal"

                    if (!uniqueIds.contains(id)) {
                        uniqueIds.add(id)
                        val contentUri = ContentUris.withAppendedId(queryUri, id).toString()
                        allMediaList.add(MediaItem(id, contentUri, dateAdded, bucketName))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        loadPhotosView()
    }

    private fun loadPhotosView() {
        recyclerView.adapter = GalleryAdapter(allMediaList)
    }

    private fun loadAlbumsView() {
        // Distinct Albums View
        val distinctAlbums = allMediaList.distinctBy { it.bucketName }
        recyclerView.adapter = GalleryAdapter(distinctAlbums)
    }

    private fun loadBinView() {
        // 30-Day Trash Empty Placeholder View
        recyclerView.adapter = GalleryAdapter(emptyList())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadDeviceMedia()
        } else {
            Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show()
        }
    }
}
