package com.smartgallery.app

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.ui.MediaAdapter
import com.smartgallery.app.ui.MediaFilterHelper
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnSettings: ImageButton
    private lateinit var mediaAdapter: MediaAdapter
    private val STORAGE_PERMISSION_CODE = 101
    private var allMediaList = listOf<MediaFilterHelper.MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewMedia)
        btnSettings = findViewById(R.id.btnSettings)

        // 3 Grid Layout for Normal Gallery Style
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        // Adapter with Click Listener for Full Screen Frame Preview
        mediaAdapter = MediaAdapter(emptyList()) { mediaItem ->
            openFullScreenPreview(mediaItem.filePath)
        }
        recyclerView.adapter = mediaAdapter

        // Top 3-Dot Menu Button Click
        btnSettings.setOnClickListener { view ->
            showSettingsMenu(view)
        }

        if (checkStoragePermission()) {
            loadDeviceMedia()
        } else {
            requestStoragePermission()
        }

        // Floating Pill Tabs (Photos / Albums) Click Handling
        findViewById<TextView>(R.id.tabPhotos)?.setOnClickListener {
            mediaAdapter.updateList(allMediaList)
        }

        findViewById<TextView>(R.id.tabAlbums)?.setOnClickListener {
            // Show Albums View Logic
        }
    }

    // 1. Full Screen Frame Photo Preview Function
    private fun openFullScreenPreview(imagePath: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this)

        val bitmap = BitmapFactory.decodeFile(imagePath)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        }

        // Photo ko poori screen par full frame dikhane ke liye CENTER_CROP
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setBackgroundColor(Color.BLACK)

        dialog.setContentView(imageView)
        imageView.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    // 2. 3-Dot Settings Menu (Recovery Box / Trash Access)
    private fun showSettingsMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menu.add(0, 1, 0, "24-Hour Recovery Box (Trash)")
        popup.menu.add(0, 2, 1, "Settings")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    // 24H Recovery Box Activity Open
                    val intent = Intent(this, com.smartgallery.app.trash.TrashActivity::class.java)
                    startActivity(intent)
                    true
                }
                2 -> true
                else -> false
            }
        }
        popup.show()
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        ActivityCompat.requestPermissions(this, arrayOf(permission), STORAGE_PERMISSION_CODE)
    }

    private fun loadDeviceMedia() {
        // Device se media files fetch karke grid me load karne ka logic
    }
}
