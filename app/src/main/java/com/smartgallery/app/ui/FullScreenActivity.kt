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

        val mediaUriStr = intent.getStringExtra("MEDIA_URI") ?: ""
        val mediaId = intent.getLongExtra("MEDIA_ID", 0L)
        val mediaFolder = intent.getStringExtra("MEDIA_FOLDER") ?: "Camera"
        val mediaDate = intent.getLongExtra("MEDIA_DATE", 0L) * 1000L

        if (mediaUriStr.isNotEmpty()) {
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

        // 1. Share Button
        btnShare.setOnClickListener {
            if (mediaUriStr.isNotEmpty()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(mediaUriStr))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
        }

        // 2. Favorite Toggle
        btnFavorite.setOnClickListener {
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show()
        }

        // 3. Edit Button
        btnEdit.setOnClickListener {
            Toast.makeText(this, "Opening Editor", Toast.LENGTH_SHORT).show()
        }

        // 4. Delete Button (Moves to 30-Day Recently Deleted Bin)
        btnDelete.setOnClickListener {
            if (mediaUriStr.isNotEmpty()) {
                val itemToDelete = MediaItem(mediaId, mediaUriStr, mediaDate / 1000L, mediaFolder)
                TrashStorage.binList.add(itemToDelete)
                Toast.makeText(this, "Moved to Recently Deleted (30 Days)", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // 5. Info Button Toggle
        var showInfo = true
        btnInfo.setOnClickListener {
            showInfo = !showInfo
            infoText.visibility = if (showInfo) View.VISIBLE else View.GONE
        }
    }
}
