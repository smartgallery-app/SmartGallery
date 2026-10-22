package com.smartgallery.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class TrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        val btnBack = findViewById<ImageButton>(R.id.btnTrashBack)
        val btnMenu = findViewById<ImageButton>(R.id.btnTrashMenu)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTrash)
        val layoutEmptyState = findViewById<LinearLayout>(R.id.layoutEmptyState)

        btnBack?.setOnClickListener { finish() }

        // Sample check for empty trash list
        val isTrashEmpty = true 

        if (isTrashEmpty) {
            layoutEmptyState?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
        } else {
            layoutEmptyState?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            recyclerView?.layoutManager = GridLayoutManager(this, 3)
        }
    }
}
