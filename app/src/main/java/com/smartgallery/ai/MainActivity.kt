package com.smartgallery.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            recyclerView = findViewById(R.id.recyclerView)
            recyclerView.layoutManager = GridLayoutManager(this, 4)

            checkPermissionsAndLoadMedia()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
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
        val mediaList = ArrayList<String>()
        try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            val cursor = contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )

            cursor?.use {
                val dataIndex = it.getColumnIndex(MediaStore.MediaColumns.DATA)
                if (dataIndex != -1) {
                    while (it.moveToNext()) {
                        val path = it.getString(dataIndex)
                        if (path != null && File(path).exists()) {
                            mediaList.add(path)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (mediaList.isNotEmpty()) {
            recyclerView.adapter = MediaAdapter(mediaList)
        } else {
            Toast.makeText(this, "No media found in storage", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show()
        }
    }

    // Simple Adapter to load images into grid
    class MediaAdapter(private val items: List<String>) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = ImageView(view.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    250
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ImageView(parent.context))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val path = items[position]
            Glide.with(holder.imageView.context)
                .load(File(path))
                .into(holder.imageView)
        }

        override fun getItemCount() = items.size
    }
}
