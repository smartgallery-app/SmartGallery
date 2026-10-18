package com.smartgallery.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R
import java.text.SimpleDateFormat
import java.util.*

class FullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        val imageView = findViewById<ImageView>(R.id.fullImageView)
        val infoText = findViewById<TextView>(R.id.infoTextView)
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        val shareButton = findViewById<Button>(R.id.shareButton)

        val mediaUriStr = intent.getStringExtra("MEDIA_URI")
        val mediaId = intent.getLongExtra("MEDIA_ID", 0)
        val mediaFolder = intent.getStringExtra("MEDIA_FOLDER") ?: "Gallery"
        val mediaDate = intent.getLongExtra("MEDIA_DATE", 0) * 1000L

        var mediaUri: Uri? = null
        if (mediaUriStr != null) {
            mediaUri = Uri.parse(mediaUriStr)
            imageView.setImageURI(mediaUri)
        }

        val formattedDate = if (mediaDate > 0) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(mediaDate))
        } else {
            "Unknown Date"
        }

        infoText.text = "Folder: $mediaFolder\nDate: $formattedDate"

        // Delete (Move to 30-Day Bin)
        deleteButton.setOnClickListener {
            if (mediaUriStr != null) {
                val itemToDelete = MediaItem(mediaId, mediaUriStr, mediaDate / 1000L, mediaFolder)
                TrashStorage.binList.add(itemToDelete)
                Toast.makeText(this, "Moved to 30-Day Bin", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Share Photo
        shareButton.setOnClickListener {
            if (mediaUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, mediaUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Photo via"))
            }
        }
    }
}
