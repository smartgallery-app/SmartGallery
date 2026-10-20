package com.smartgallery.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.smartgallery.app.R
import java.text.SimpleDateFormat
import java.util.*

class FullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        try {
            val viewPager = findViewById<ViewPager2>(R.id.viewPager)
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

            val currentItem = MediaItem(mediaId, mediaUriStr, mediaDate / 1000L, mediaFolder)
            val mediaList = listOf(currentItem)
            val adapter = MediaPagerAdapter(mediaList)
            viewPager.adapter = adapter

            val formattedDate = if (mediaDate > 0) {
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(mediaDate))
            } else {
                "Unknown Date"
            }

            infoText?.text = "Folder: $mediaFolder\nDate: $formattedDate"

            btnShare?.setOnClickListener {
                if (mediaUriStr.isNotEmpty()) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(mediaUriStr))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share via"))
                }
            }

            btnFavorite?.setOnClickListener {
                Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show()
            }

            // Edit Button - Opens PhotoEditActivity with Filters
            btnEdit?.setOnClickListener {
                if (mediaUriStr.isNotEmpty()) {
                    val intent = Intent(this, PhotoEditActivity::class.java).apply {
                        putExtra("EDIT_URI", mediaUriStr)
                    }
                    startActivity(intent)
                }
            }

            btnDelete?.setOnClickListener {
                if (mediaUriStr.isNotEmpty()) {
                    TrashStorage.binList.add(currentItem)
                    Toast.makeText(this, "Moved to Recently Deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            var showInfo = false
            btnInfo?.setOnClickListener {
                showInfo = !showInfo
                infoText?.visibility = if (showInfo) View.VISIBLE else View.GONE
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to load media", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
