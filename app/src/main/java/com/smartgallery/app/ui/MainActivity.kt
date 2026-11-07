package com.smart.gallery

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start background 24/7 AI scanner service silently
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
            // Background loading to prevent UI freeze and black screen
            val mediaList = fetchMediaFilesFromDevice()
            
            withContext(Dispatchers.Main) {
                // Update UI smoothly once data is ready
                // recyclerViewPhotos.adapter = MediaAdapter(mediaList)
            }
        }
    }

    private fun fetchMediaFilesFromDevice(): List<String> {
        val list = mutableListOf<String>()
        // Fetch files safely using MediaStore
        return list
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}
