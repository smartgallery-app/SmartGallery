package com.smartgallery.app.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class TrashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TrashActivity"
    }

    private lateinit var recyclerViewTrash: RecyclerView
    private val trashList = mutableListOf<MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        recyclerViewTrash = findViewById(R.id.recyclerViewTrash)
        recyclerViewTrash?.layoutManager = GridLayoutManager(this, 3)

        loadDeletedItems()
    }

    private fun loadDeletedItems() {
        try {
            trashList.clear()
            // 30-day recovery items loading logic here
            Log.d(TAG, "Loaded ${trashList.size} items in 30-day recovery box.")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading trash items: ${e.message}")
        }
    }
}
