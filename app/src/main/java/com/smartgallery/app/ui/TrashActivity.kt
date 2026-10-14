package com.smartgallery.app.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class TrashActivity : AppCompatActivity() {

    private var recyclerViewTrash: RecyclerView? = null
    private var tvEmptyTrash: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_trash)

            recyclerViewTrash = findViewById(R.id.recyclerViewTrash)
            tvEmptyTrash = findViewById(R.id.tvEmptyTrash)

            recyclerViewTrash?.layoutManager = GridLayoutManager(this, 3)
            recyclerViewTrash?.visibility = View.GONE
            tvEmptyTrash?.visibility = View.VISIBLE

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
