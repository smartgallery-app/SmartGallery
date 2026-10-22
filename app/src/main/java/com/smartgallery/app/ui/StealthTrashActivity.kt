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

        val btnBack = findViewById<ImageButton>(R.id.btnStealthBack)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewStealth)

        btnBack?.setOnClickListener { finish() }

        val adapter = TrashAdapter(TrashStorage.stealthBinList, isStealthBox = true) { item: TrashItem ->
            // Stealth Item click action
        }

        recyclerView?.layoutManager = GridLayoutManager(this, 3)
        recyclerView?.adapter = adapter
    }
}
