package com.smartgallery.app.ui

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.smartgallery.app.R

class FullScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        val imageView = findViewById<ImageView>(R.id.fullScreenImageView) // Ensure id matches activity_full_screen.xml or use standard
        val uri = intent.getStringExtra("MEDIA_URI")

        if (uri != null) {
            Glide.with(this)
                .load(uri)
                .into(imageView)
        }
    }
}
