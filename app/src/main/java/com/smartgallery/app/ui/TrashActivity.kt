package com.smartgallery.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class TrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTrash)
        recyclerView?.layoutManager = LinearLayoutManager(this)
        
        val adapter = TrashAdapter(TrashStorage.mainBinList) { item ->
            // Click action
        }
        recyclerView?.adapter = adapter
    }
}
