package com.smartgallery.ai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var repository: MediaRepository
    private lateinit var adapter: MediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val database = AppDatabase.getDatabase(this)
        repository = MediaRepository(this, database)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        adapter = MediaAdapter(emptyList()) { mediaItem ->
            // Photo/Video preview click handling
        }
        recyclerView.adapter = adapter

        loadMediaData()
    }

    private fun loadMediaData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val mediaList = repository.getAllMedia()
            withContext(Dispatchers.Main) {
                adapter.updateData(mediaList)
            }
        }
    }
}
