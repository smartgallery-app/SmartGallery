package com.smartgallery.app.ui

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R

class FullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        val imageView = findViewById<ImageView>(R.id.fullImageView)
        val mediaUri = intent.getStringExtra("MEDIA_URI")

        if (mediaUri != null) {
            imageView.setImageURI(Uri.parse(mediaUri))
        }

        imageView.setOnClickListener {
            finish() // Tap again to close full screen view
        }
    }
}
