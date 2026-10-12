package com.smartgallery.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var repository: MediaRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadMediaData()
        } else {
            Toast.makeText(this, "Permission denied to access media", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = MediaRepository(this)
        recyclerView = findViewById(R.id.recyclerViewMedia)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        mediaAdapter = MediaAdapter(emptyList()) { item ->
            Toast.makeText(this, item.fileName, Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = mediaAdapter

        checkAndRequestPermission()
    }

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadMediaData()
        } else {
            requestPermissionLauncher.launch(permission)
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
}
