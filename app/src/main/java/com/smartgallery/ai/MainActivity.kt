package com.smartgallery.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartgallery.app.R

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoPhotos: TextView
    private lateinit var btnSettings: ImageButton
    private lateinit var btnTrash: ImageButton
    private val photoList = mutableListOf<String>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewPhotos)
        tvNoPhotos = findViewById(R.id.tvNoPhotos)
        btnTrash = findViewById(R.id.btnTrash)
        btnSettings = findViewById(R.id.btnSettings)

        recyclerView.layoutManager = GridLayoutManager(this, 3)

        // Settings Button Click
        btnSettings.setOnClickListener {
            Toast.makeText(this, "Settings Menu Opening...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Trash/Delete Box Button Click
        btnTrash.setOnClickListener {
            Toast.makeText(this, "Trash Box Opening...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, TrashActivity::class.java)
            startActivity(intent)
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
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
                tvNoPhotos.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvNoPhotos.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tvNoPhotos.visibility = View.VISIBLE
            tvNoPhotos.text = "Error loading photos"
        }
    }
}

