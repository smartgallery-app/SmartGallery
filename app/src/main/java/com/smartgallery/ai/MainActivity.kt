package com.smartgallery.ai

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.smartgallery.app.R
import com.smartgallery.app.ui.MediaAdapter
import com.smartgallery.app.ui.MediaFilterHelper
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnSettings: ImageButton
    private lateinit var mediaAdapter: MediaAdapter
    private val STORAGE_PERMISSION_CODE = 101
    private var allMediaList = mutableListOf<MediaFilterHelper.MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewMedia)
        btnSettings = findViewById(R.id.btnSettings)

        // Standard Stock Gallery 3-Column Grid Layout
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        // MediaAdapter Initialization with Full Screen Click Listener
        mediaAdapter = MediaAdapter(emptyList()) { mediaItem ->
            openFullScreenPreview(mediaItem.filePath)
        }
        recyclerView.adapter = mediaAdapter

        // Settings / Recovery Box Click
        btnSettings.setOnClickListener { view ->
            showSettingsMenu(view)
        }

        if (checkStoragePermission()) {
            loadDeviceMedia()
        } else {
            requestStoragePermission()
        }

        // Bottom Navigation Tabs
        findViewById<TextView>(R.id.tabPhotos)?.setOnClickListener {
            mediaAdapter.updateList(allMediaList)
        }

        findViewById<TextView>(R.id.tabAlbums)?.setOnClickListener {
            // Folder view logic
        }
    }

    // Borderless Full-Screen Image Preview
    private fun openFullScreenPreview(imagePath: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this)

        val bitmap = BitmapFactory.decodeFile(imagePath)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        }

        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setBackgroundColor(Color.BLACK)

        dialog.setContentView(imageView)
        imageView.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    // Top Right Menu
    private fun showSettingsMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menu.add(0, 1, 0, "24-Hour Recovery Box (Trash)")
        popup.menu.add(0, 2, 1, "Settings & Auto-Delete Options")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> true
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

    // Complete Safe Device Media Loader
    private fun loadDeviceMedia() {
        allMediaList.clear()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val path = it.getString(dataColumn)
                if (path != null) {
                    val fileObj = File(path)
                    if (fileObj.exists()) {
                        val mediaItem = MediaFilterHelper.MediaItem(
                            filePath = path,
                            file = fileObj,
                            uri = android.net.Uri.fromFile(fileObj),
                            category = "NORMAL",
                            timestamp = fileObj.lastModified()
                        )
                        allMediaList.add(mediaItem)
                    }
                }
            }
        }
        mediaAdapter.updateList(allMediaList)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadDeviceMedia()
            }
        }
    }
}
