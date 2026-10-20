package com.smartgallery.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class TrashActivity : AppCompatActivity() {

    private lateinit var adapter: TrashAdapter
    private val mainTrashList = mutableListOf<MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTrash)
        val btnClearAll = findViewById<Button>(R.id.btnClearAll)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // 30-Day Limit Logic (30 * 24 * 60 * 60 * 1000L)
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        TrashStorage.mainBinList.removeAll { item ->
            (currentTime - item.dateAdded * 1000L) > thirtyDaysInMillis
        }

        mainTrashList.addAll(TrashStorage.mainBinList)

        adapter = TrashAdapter(
            mainTrashList,
            onRestoreClick = { item ->
                TrashStorage.mainBinList.remove(item)
                mainTrashList.remove(item)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "File Restored to Gallery", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { item ->
                TrashStorage.mainBinList.remove(item)
                mainTrashList.remove(item)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "File Permanently Deleted", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.adapter = adapter

        btnClearAll.setOnClickListener {
            TrashStorage.mainBinList.clear()
            mainTrashList.clear()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Trash Cleared!", Toast.LENGTH_SHORT).show()
        }
    }
}
