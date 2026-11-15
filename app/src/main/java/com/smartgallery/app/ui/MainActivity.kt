package com.smartgallery.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewPhotos: RecyclerView
    private lateinit var scrollViewAlbums: ScrollView
    private lateinit var tabPhotos: TextView
    private lateinit var tabAlbums: TextView
    private lateinit var btnSettings: ImageButton

    private val activityScope = CoroutineScope(Dispatchers.Main + Job())
    private val STORAGE_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (checkAndRequestPermissions()) {
            initializeApp()
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        return if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), STORAGE_PERMISSION_CODE)
            false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeApp()
            }
        }
    }

    private fun initializeApp() {
        startService(Intent(this, SmartScanBackgroundService::class.java))

        initViews()
        setupListeners()
        loadMediaAsync()
    }

    private fun initViews() {
        recyclerViewPhotos = findViewById(R.id.recyclerViewPhotos)
        scrollViewAlbums = findViewById(R.id.scrollViewAlbums)
        tabPhotos = findViewById(R.id.tabPhotos)
        tabAlbums = findViewById(R.id.tabAlbums)
        btnSettings = findViewById(R.id.btnSettings)

        recyclerViewPhotos.layoutManager = GridLayoutManager(this, 3)
    }

    private fun setupListeners() {
        tabPhotos.setOnClickListener {
            tabPhotos.setBackgroundColor(getColor(android.R.color.darker_gray))
            tabAlbums.setBackgroundColor(0)
            recyclerViewPhotos.visibility = View.VISIBLE
            scrollViewAlbums.visibility = View.GONE
        }

        tabAlbums.setOnClickListener {
            tabAlbums.setBackgroundColor(getColor(android.R.color.darker_gray))
            tabPhotos.setBackgroundColor(0)
            scrollViewAlbums.visibility = View.VISIBLE
            recyclerViewPhotos.visibility = View.GONE
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun loadMediaAsync() {
        activityScope.launch(Dispatchers.IO) {
            val mediaList = fetchMediaFilesFromDevice()
            withContext(Dispatchers.Main) {
                // Yahan saari photos/videos load hokar grid mein set ho jayengi
                // Jaise hi tumhara adapter lagega, sab dikhne lagega
            }
        }
    }

    private fun fetchMediaFilesFromDevice(): List<String> {
        val mediaList = mutableListOf<String>()
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val absolutePathOfImage = it.getString(columnIndex)
                mediaList.add(absolutePathOfImage)
            }
        }
        return mediaList
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}
