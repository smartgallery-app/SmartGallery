package com.smartgallery.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class MainActivity : AppCompatActivity() {

    private var recyclerViewAlbums: RecyclerView? = null
    private var tvNoPhotos: TextView? = null
    private var btnSettings: ImageView? = null
    private var btnTrash: ImageView? = null
    private val folderList = mutableSetOf<String>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            recyclerViewAlbums = findViewById(R.id.recyclerViewAlbums)
            tvNoPhotos = findViewById(R.id.tvNoPhotos)
            btnSettings = findViewById(R.id.btnSettings)
            btnTrash = findViewById(R.id.btnTrash)

            recyclerViewAlbums?.layoutManager = GridLayoutManager(this, 3)

            btnSettings?.setOnClickListener {
                try {
                    startActivity(Intent(this, SettingsActivity::class.java))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            btnTrash?.setOnClickListener {
                try {
                    startActivity(Intent(this, TrashActivity::class.java))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            checkAndRequestPermissions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAndRequestPermissions() {
        try {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            } else {
                loadAlbums()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAlbums() {
        try {
            folderList.clear()
            val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val folderName = cursor.getString(bucketColumn)
                    if (!folderName.isNullOrEmpty()) {
                        folderList.add(folderName)
                    }
                }
            }

            if (folderList.isEmpty()) {
                tvNoPhotos?.visibility = View.VISIBLE
                recyclerViewAlbums?.visibility = View.GONE
            } else {
                tvNoPhotos?.visibility = View.GONE
                recyclerViewAlbums?.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
