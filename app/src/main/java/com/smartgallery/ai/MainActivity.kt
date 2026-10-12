package com.smartgallery.ai

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var repository: MediaRepository
    private lateinit var btnDeleteWhatsApp: Button

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            loadMediaData()
        } else {
            Toast.makeText(this, "Permissions deny hone par photos aur delete feature kaam nahi karega!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = MediaRepository(this)
        recyclerView = findViewById(R.id.recyclerViewMedia)
        btnDeleteWhatsApp = findViewById(R.id.btnDeleteWhatsApp)

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        
        // Photo click par Full Screen mein kholna
        mediaAdapter = MediaAdapter(emptyList()) { item ->
            showFullScreenImage(item.uri)
        }
        recyclerView.adapter = mediaAdapter

        btnDeleteWhatsApp.setOnClickListener {
            confirmAndDeleteWhatsAppMedia()
        }

        checkAndRequestAllPermissions()
    }

    private fun showFullScreenImage(imageUri: android.net.Uri) {
        val builder = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this)
        imageView.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        
        Glide.with(this).load(imageUri).into(imageView)
        
        builder.setView(imageView)
        val dialog = builder.create()
        imageView.setOnClickListener { dialog.dismiss() } // Click karne par image close ho jayegi
        dialog.show()
    }

    private fun checkAndRequestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            loadMediaData()
        }
    }

    private fun loadMediaData() {
        CoroutineScope(Dispatchers.IO).launch {
            val mediaList = repository.getAllMedia()
            withContext(Dispatchers.Main) {
                mediaAdapter.updateData(mediaList)
            }
        }
    }

    private fun confirmAndDeleteWhatsAppMedia() {
        AlertDialog.Builder(this)
            .setTitle("Clean WhatsApp Media")
            .setMessage("Kya aap WhatsApp ke saare Screenshots, Photos aur Videos permanently delete karna chahte hain?")
            .setPositiveButton("Delete All") { _, _ ->
                deleteWhatsAppMediaAndScreenshots()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteWhatsAppMediaAndScreenshots() {
        CoroutineScope(Dispatchers.IO).launch {
            var totalDeleted = 0

            // 1. Delete WhatsApp Images & Screenshots
            totalDeleted += deleteFromUri(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Images.Media.DATA} LIKE ? OR ${MediaStore.Images.Media.DATA} LIKE ?",
                arrayOf("%WhatsApp%", "%Screenshot%")
            )

            // 2. Delete WhatsApp Videos
            totalDeleted += deleteFromUri(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Video.Media.DATA} LIKE ?",
                arrayOf("%WhatsApp%")
            )

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "$totalDeleted WhatsApp media & screenshots deleted successfully!", Toast.LENGTH_LONG).show()
                loadMediaData()
            }
        }
    }

    private fun deleteFromUri(contentUri: android.net.Uri, selection: String, selectionArgs: Array<String>): Int {
        var count = 0
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val cursor = contentResolver.query(contentUri, projection, selection, selectionArgs, null)

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val deleteUri = ContentUris.withAppendedId(contentUri, id)
                try {
                    val rows = contentResolver.delete(deleteUri, null, null)
                    if (rows > 0) count++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return count
    }
}
