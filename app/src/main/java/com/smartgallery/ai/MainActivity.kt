package com.smartgallery.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R
import com.smartgallery.app.MediaFilterHelper
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val PERMISSION_REQUEST_CODE = 100
    
    // Video frame analysis interval set to 3-5 seconds (4000 ms)
    private val FRAME_INTERVAL_MS = 4000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        // Display par photos ki sankhya badal kar 4 columns kar di hai
        recyclerView.layoutManager = GridLayoutManager(this, 4)

        checkPermissionsAndLoadMedia()
    }

    private fun checkPermissionsAndLoadMedia() {
        val permissionsList = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsList.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missingPermissions = permissionsList.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            loadMedia()
        }
    }

    private fun loadMedia() {
        val mediaList = ArrayList<MediaFilterHelper.MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE
        )

        val cursor = contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )

        cursor?.use {
            val dataIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val mimeIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

            while (it.moveToNext()) {
                val path = it.getString(dataIndex)
                val mimeType = it.getString(mimeIndex) ?: ""

                if (path != null && File(path).exists()) {
                    val isVideo = mimeType.startsWith("video")
                    mediaList.add(MediaFilterHelper.MediaItem(path, isVideo))
                }
            }
        }

        recyclerView.adapter = MediaAdapter(mediaList) { item ->
            // Photo/Video par tap karne par full screen view ka action
            Toast.makeText(this, "Opening full screen media...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            loadMedia()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}
