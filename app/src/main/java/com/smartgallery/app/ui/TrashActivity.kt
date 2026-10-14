package com.smartgallery.app.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class TrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        val tvEmptyTrashText: TextView = findViewById(R.id.tvEmptyTrashText)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewTrash)

        tvEmptyTrashText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }
}
