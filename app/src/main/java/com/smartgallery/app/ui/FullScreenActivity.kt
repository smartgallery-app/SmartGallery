package com.smartgallery.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.smartgallery.app.R
import java.text.SimpleDateFormat
import java.util.*

class FullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        val imageView = findViewById<ImageView>(R.id.fullImageView)
        val infoText = findViewById<TextView>(R.id.infoTextView)
        
        val btnShare = findViewById<MaterialButton>(R.id.btnShare)
        val btnFavorite = findViewById<MaterialButton>(R.id.btnFavorite)
        val btnEdit = findViewById<MaterialButton>(R.id.btnEdit)
        val btnDelete = findViewById<MaterialButton>(R.id.btnDelete)
        val btnInfo = findViewById<MaterialButton>(R.id.btnInfo)

        val mediaUriStr = intent.getStringExtra("MEDIA_URI")
        val mediaId = intent.getLongExtra("MEDIA_ID", 0L)
        val mediaFolder = intent.getStringExtra("MEDIA_FOLDER") ?: "Camera"
        val mediaDate = intent.getLongExtra("MEDIA_DATE", 0L) * 1000L

        if (!mediaUriStr.isNull_or_empty()) {
            try {
                imageView.setImageURI(Uri.parse(mediaUriStr))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val formattedDate = if (mediaDate > 0) {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(mediaDate))
        } else {
            "Unknown Date"
        }

        infoText.text = "Folder: $mediaFolder\nDate: $formattedDate"

        // Share Button
        btnShare.setOnClickListener {
            if (!mediaUriStr.isNull_or_empty()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(mediaUriStr))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
        }

        // Favorite Toggle
        btnFavorite.setOnClickListener {
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show()
        }

        // Edit
        btnEdit.setOnClickListener {
            Toast.makeText(this, "Photo Editor feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Delete (Moves to 30-Day Bin)
        btnDelete.setOnClickListener {
            if (!mediaUriStr.isNull_or_empty()) {
                val itemToDelete = MediaItem(mediaId, mediaUriStr, mediaDate / 1000L, mediaFolder)
                TrashStorage.binList.add(itemToDelete)
                Toast.makeText(this, "30 Din ke Recycle Bin me bhej diya gaya hai", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Info Toggle
        btnInfo.setOnClickListener {
            infoText.visibility = if (infoText.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private fun String?.isNull_or_empty(): Boolean {
        return this == null || this.isEmpty()
    }
}
