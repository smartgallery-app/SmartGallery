package com.smartgallery.app.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class AlbumDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_detail)

        val albumName = intent.getStringExtra("album_name") ?: "Album"
        val tvTitle = findViewById<TextView>(R.id.tvAlbumDetailTitle)
        tvTitle?.text = albumName

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewAlbumDetail)
        recyclerView?.layoutManager = GridLayoutManager(this, 3)
        
        // Aap yahan album ke andar ke items load kar sakte hain
    }
}
