package com.smartgallery.app.ui

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.smartgallery.app.R

class FullScreenViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        val imageView = findViewById<ImageView>(R.id.imageViewFullScreen)
        val mediaUri = intent.getStringExtra("media_uri")

        if (!mediaUri.isNullOrEmpty()) {
            Glide.with(this)
                .load(mediaUri)
                .into(imageView)
            }
        
        imageView?.setOnClickListener {
            finish()
        }
    }
}
