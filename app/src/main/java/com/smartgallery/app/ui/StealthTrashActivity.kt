package com.smartgallery.app.ui

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class StealthTrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stealth_trash)

        // Safe ID check for Back button
        val btnBack = findViewById<ImageButton>(R.id.btnStealthBack) 
            ?: findViewById<ImageButton>(R.id.btnTrashBack)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStealth)
            ?: findViewById<RecyclerView>(R.id.recyclerViewTrash)

        btnBack?.setOnClickListener { finish() }

        val adapter = TrashAdapter(TrashStorage.stealthBinList, isStealthBox = true) { item: TrashItem ->
            // Click action
        }

        recyclerView?.layoutManager = GridLayoutManager(this, 3)
        recyclerView?.adapter = adapter
    }
}
