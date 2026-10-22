package com.smartgallery.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class StealthTrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stealth_trash)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStealth)
        recyclerView?.layoutManager = GridLayoutManager(this, 3)

        val adapter = TrashAdapter(TrashStorage.stealthBinList) { item ->
            // Click handler
        }
        recyclerView?.adapter = adapter
    }
}
