package com.smartgallery.app.ui

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R
import java.text.SimpleDateFormat
import java.util.*

class FullScreenActivity : AppCompatActivity() {

    private var isZoomed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        val imageView = findViewById<ImageView>(R.id.fullImageView)
        val infoText = findViewById<TextView>(R.id.infoTextView)

        val mediaUri = intent.getStringExtra("MEDIA_URI")
        val mediaFolder = intent.getStringExtra("MEDIA_FOLDER") ?: "Gallery"
        val mediaDate = intent.getLongExtra("MEDIA_DATE", 0) * 1000L

        if (mediaUri != null) {
            imageView.setImageURI(Uri.parse(mediaUri))
        }

        val formattedDate = if (mediaDate > 0) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(mediaDate))
        } else {
            "Unknown Date"
        }

        infoText.text = "Folder: $mediaFolder\nDate: $formattedDate"

        // Double tap or click zoom toggle support
        imageView.setOnClickListener {
            isZoomed = !isZoomed
            if (isZoomed) {
                imageView.scaleX = 2.0f
                imageView.scaleY = 2.0f
            } else {
                imageView.scaleX = 1.0f
                imageView.scaleY = 1.0f
            }
        }
    }
}
