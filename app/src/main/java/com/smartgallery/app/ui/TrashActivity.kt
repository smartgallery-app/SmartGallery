package com.smartgallery.app.ui

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class TrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        val btnBack = findViewById<ImageButton>(R.id.btnTrashBack)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTrash)

        btnBack?.setOnClickListener { finish() }

        val adapter = TrashAdapter(TrashStorage.mainBinList, isStealthBox = false) { item: TrashItem -> }

        recyclerView?.layoutManager = GridLayoutManager(this, 3)
        recyclerView?.adapter = adapter
    }
}
