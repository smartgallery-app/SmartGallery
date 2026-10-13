package com.smartgallery.app

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.smartgallery.app.ui.MediaAdapter
import com.smartgallery.app.ui.MediaFilterHelper

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var btnSettings: ImageButton
    private lateinit var mediaAdapter: MediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewMedia)
        chipGroupFilters = findViewById(R.id.chipGroupFilters)
        btnSettings = findViewById(R.id.btnSettings)

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        mediaAdapter = MediaAdapter(emptyList())
        recyclerView.adapter = mediaAdapter
    }
}
