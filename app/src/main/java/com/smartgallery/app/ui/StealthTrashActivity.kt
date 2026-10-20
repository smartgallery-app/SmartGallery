package com.smartgallery.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class StealthTrashActivity : AppCompatActivity() {

    private lateinit var adapter: TrashAdapter
    private val stealthList = mutableListOf<MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stealth_trash)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStealthTrash)
        val btnClearStealth = findViewById<Button>(R.id.btnClearStealth)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // 12-Hour Limit Logic (12 * 60 * 60 * 1000L)
        val twelveHoursInMillis = 12L * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        TrashStorage.stealthBinList.removeAll { item ->
            (currentTime - item.dateAdded * 1000L) > twelveHoursInMillis
        }

        stealthList.addAll(TrashStorage.stealthBinList)

        adapter = TrashAdapter(
            stealthList,
            onRestoreClick = { item ->
                TrashStorage.stealthBinList.remove(item)
                stealthList.remove(item)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Item Restored to Gallery", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { item ->
                TrashStorage.stealthBinList.remove(item)
                stealthList.remove(item)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Item Permanently Deleted", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerView.adapter = adapter

        btnClearStealth.setOnClickListener {
            TrashStorage.stealthBinList.clear()
            stealthList.clear()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Stealth Trash Cleared!", Toast.LENGTH_SHORT).show()
        }
    }
}
