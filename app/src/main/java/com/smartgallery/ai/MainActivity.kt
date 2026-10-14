package com.smartgallery.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class MainActivity : AppCompatActivity() {

    private var recyclerView: RecyclerView? = null
    private var tvNoPhotos: TextView? = null
    private var btnSettings: ImageView? = null
    private var btnTrash: ImageView? = null
    private val photoList = mutableListOf<String>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            recyclerView = findViewById(R.id.recyclerViewPhotos)
            tvNoPhotos = findViewById(R.id.tvNoPhotos)
            btnTrash = findViewById(R.id.btnTrash)
            btnSettings = findViewById(R.id.btnSettings)

            recyclerView?.layoutManager = GridLayoutManager(this, 3)

            btnSettings?.setOnClickListener {
                startActivity(Intent(this, SettingsActivity::class.java))
            }

            btnTrash?.setOnClickListener {
                startActivity(Intent(this, TrashActivity::class.java))
            }

            checkAndRequestPermissions()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Opening Gallery...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
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
            loadPhotos()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadPhotos()
            } else {
                tvNoPhotos?.visibility = View.VISIBLE
                tvNoPhotos?.text = "Storage Permission Required"
            }
        }
    }

    private fun loadPhotos() {
        photoList.clear()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()
                    photoList.add(contentUri)
                }
            }

            if (photoList.isEmpty()) {
                tvNoPhotos?.visibility = View.VISIBLE
                recyclerView?.visibility = View.GONE
            } else {
                tvNoPhotos?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tvNoPhotos?.visibility = View.VISIBLE
            tvNoPhotos?.text = "Error loading photos"
        }
    }
}
