package com.smartgallery.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.smartgallery.app.R

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var selectionActionBar: LinearLayout
    private lateinit var btnMultiShare: MaterialButton
    private lateinit var btnMultiDelete: MaterialButton

    private val allMediaList = ArrayList<MediaItem>()
    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerView)
        bottomNav = findViewById(R.id.bottomNavigation)
        selectionActionBar = findViewById(R.id.selectionActionBar)
        btnMultiShare = findViewById(R.id.btnMultiShare)
        btnMultiDelete = findViewById(R.id.btnMultiDelete)

        recyclerView.layoutManager = GridLayoutManager(this, 3)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_photos -> {
                    supportActionBar?.title = "Photos"
                    recyclerView.layoutManager = GridLayoutManager(this, 3)
                    loadPhotosView()
                    true
                }
                R.id.nav_albums -> {
                    supportActionBar?.title = "Albums"
                    recyclerView.layoutManager = GridLayoutManager(this, 3)
                    loadAlbumsView()
                    true
                }
                else -> false
            }
        }

        checkAndRequestPermissions()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 101, 0, "Search")?.setIcon(android.R.drawable.ic_menu_search)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu?.add(0, 102, 1, "Sort by")
        menu?.add(0, 103, 2, "Settings")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            103 -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadDeviceMedia()
        } else {
            Toast.makeText(this, "Permission required to view photos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDeviceMedia() {
        allMediaList.clear()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        try {
            contentResolver.query(queryUri, projection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val bucketName = cursor.getString(bucketCol) ?: "Camera"
                    val contentUri = ContentUris.withAppendedId(queryUri, id).toString()
                    allMediaList.add(MediaItem(id, contentUri, dateAdded, bucketName))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        loadPhotosView()
    }

    private fun loadPhotosView() {
        selectionActionBar.visibility = View.GONE
        val adapter = GalleryAdapter(allMediaList, isAlbumView = false) { selectedCount ->
            if (selectedCount > 0) {
                selectionActionBar.visibility = View.VISIBLE
                supportActionBar?.title = "$selectedCount Selected"
            } else {
                selectionActionBar.visibility = View.GONE
                supportActionBar?.title = "Photos"
            }
        }
        recyclerView.adapter = adapter

        btnMultiDelete.setOnClickListener {
            val selectedPositions = adapter.selectedItems.toList().sortedDescending()
            for (pos in selectedPositions) {
                if (pos < allMediaList.size) {
                    TrashStorage.binList.add(allMediaList[pos])
                    allMediaList.removeAt(pos)
                }
            }
            adapter.selectedItems.clear()
            adapter.isMultiSelectMode = false
            adapter.notifyDataSetChanged()
            selectionActionBar.visibility = View.GONE
            supportActionBar?.title = "Photos"
            Toast.makeText(this, "Moved to Recently Deleted", Toast.LENGTH_SHORT).show()
        }

        btnMultiShare.setOnClickListener {
            val uris = ArrayList<android.net.Uri>()
            for (pos in adapter.selectedItems) {
                if (pos < allMediaList.size) {
                    uris.add(android.net.Uri.parse(allMediaList[pos].uri))
                }
            }
            if (uris.isNotEmpty()) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
        }
    }

    private fun loadAlbumsView() {
        selectionActionBar.visibility = View.GONE
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = AlbumsAdapter(allMediaList) { albumName ->
            if (albumName == "Recently deleted") {
                val trashAdapter = GalleryAdapter(TrashStorage.binList, isAlbumView = true)
                recyclerView.adapter = trashAdapter
                supportActionBar?.title = "Recently Deleted"
            } else if (albumName == "Recent") {
                loadPhotosView()
                supportActionBar?.title = "Photos"
            } else {
                val filteredList = allMediaList.filter { it.bucketName == albumName }
                val folderAdapter = GalleryAdapter(filteredList, isAlbumView = true)
                recyclerView.adapter = folderAdapter
                supportActionBar?.title = albumName
            }
        }
    }
}
