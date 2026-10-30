package com.smartgallery.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.smartgallery.app.R

class FullScreenViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        val imageView = findViewById<ImageView>(R.id.imageViewFullScreen)
        val infoTextView = findViewById<TextView>(R.id.infoTextView)
        val mediaUri = intent.getStringExtra("media_uri")

        if (!mediaUri.isNullOrEmpty()) {
            Glide.with(this)
                .load(mediaUri)
                .into(imageView)
        }

        imageView?.setOnClickListener {
            finish()
        }

        val btnInfo = findViewById<MaterialButton>(R.id.btnInfo)
        btnInfo?.setOnClickListener {
            if (infoTextView != null) {
                if (infoTextView.visibility == View.VISIBLE) {
                    infoTextView.visibility = View.GONE
                } else {
                    infoTextView.visibility = View.VISIBLE
                    infoTextView.text = "URI: $mediaUri"
                }
            }
        }
    }
}
