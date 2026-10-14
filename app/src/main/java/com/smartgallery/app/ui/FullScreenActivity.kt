package com.smartgallery.app.ui

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R

class FullScreenActivity : AppCompatActivity() {

    private var imageView: ImageView? = null
    private var btnBack: ImageView? = null
    private var btnDelete: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_full_screen)

            imageView = findViewById(R.id.fullScreenImageView)
            btnBack = findViewById(R.id.btnBack)
            btnDelete = findViewById(R.id.btnDeletePhoto)

            val photoUriString = intent.getStringExtra("photo_uri")
            if (photoUriString != null) {
                val uri = Uri.parse(photoUriString)
                imageView?.setImageURI(uri)
            }

            btnBack?.setOnClickListener {
                finish()
            }

            btnDelete?.setOnClickListener {
                finish()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
