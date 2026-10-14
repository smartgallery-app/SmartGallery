package com.example.smartgallery

import android.Manifest
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

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoPhotos: TextView
    private lateinit var adapter: PhotoAdapter
    private val photoList = mutableListOf<String>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewPhotos)
        tvNoPhotos = findViewById(R.id.tvNoPhotos)
        val btnTrash: ImageButton = findViewById(R.id.btnTrash)
        val btnSettings: ImageButton = findViewById(R.id.btnSettings)

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = PhotoAdapter(photoList) { imagePath ->
            val intent = Intent(this, FullScreenActivity::class.java)
            intent.putExtra("IMAGE_PATH", imagePath)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadPhotos()
            } else {
                tvNoPhotos.visibility = View.VISIBLE
                tvNoPhotos.text = "Permission denied. Allow storage access in Settings."
            }
        }
    }

    private fun loadPhotos() {
        photoList.clear()
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                while (it.moveToNext()) {
                    val path = it.getString(columnIndex)
                    if (path != null) {
                        photoList.add(path)
                    }
                }
            }

            if (photoList.isEmpty()) {
                tvNoPhotos.visibility = View.VISIBLE
            } else {
                tvNoPhotos.visibility = View.GONE
                adapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tvNoPhotos.visibility = View.VISIBLE
            tvNoPhotos.text = "Error loading photos"
        }
    }
}
