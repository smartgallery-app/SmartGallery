package com.smartgallery.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Toast.makeText(this, "Permissions Granted! Scanning Gallery...", Toast.LENGTH_SHORT).show()
            loadGalleryImages()
        } else {
            Toast.makeText(this, "Permissions Denied. App needs storage access.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        statusTextView = TextView(this).apply {
            text = "SmartGallery AI\nGallery Scanner & Manager\n\nChecking Permissions..."
            textSize = 18f
            setPadding(50, 100, 50, 50)
            setContentView(this)
        }

        checkAndRequestGalleryPermissions()
    }

    private fun checkAndRequestGalleryPermissions() {
        val permissionsList = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsList.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissionsList.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsList.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val notGranted = permissionsList.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        } else {
            Toast.makeText(this, "Gallery Permission Already Granted", Toast.LENGTH_SHORT).show()
            loadGalleryImages()
        }
    }

    private fun loadGalleryImages() {
        val mediaList = MediaRepository.loadGalleryMedia(this)
        statusTextView.text = "SmartGallery AI\n\nTotal Photos Found: ${mediaList.size}\n\nLocal Privacy Mode Active (No Cloud Upload)"
    }
}
