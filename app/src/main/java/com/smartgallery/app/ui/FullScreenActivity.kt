package com.smartgallery.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.smartgallery.app.R

class FullScreenActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private val mediaList = mutableListOf<MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_pager)

        val btnBack = findViewById<ImageButton>(R.id.btnBackFull)
        val btnShare = findViewById<LinearLayout>(R.id.btnShareSingle)
        val btnDelete = findViewById<LinearLayout>(R.id.btnDeleteSingle)
        viewPager = findViewById(R.id.viewPagerMedia)

        val mediaUri = intent.getStringExtra("media_uri") ?: ""
        if (mediaUri.isNotEmpty()) {
            mediaList.add(MediaItem(0, mediaUri, 0, ""))
        }

        val adapter = FullScreenPagerAdapter(mediaList)
        viewPager.adapter = adapter

        btnBack?.setOnClickListener { finish() }

        btnShare?.setOnClickListener {
            val currentPos = viewPager.currentItem
            if (mediaList.isNotEmpty()) {
                val item = mediaList[currentPos]
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
                }
                startActivity(Intent.createChooser(shareIntent, "Share Image"))
            }
        }

        btnDelete?.setOnClickListener {
            // Move to 30-Day Main Trash logic
            finish()
        }
    }
}
