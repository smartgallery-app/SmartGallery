package com.smartgallery.app.ui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.smartgallery.app.R

class FullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_view)

        val imgFullScreen = findViewById<ImageView>(R.id.imgFullScreen)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        val mediaUri = intent.getStringExtra("media_uri")

        if (mediaUri != null) {
            Glide.with(this)
                .load(mediaUri)
                .into(imgFullScreen)
        }

        btnBack?.setOnClickListener {
            finish()
        }
    }
}
