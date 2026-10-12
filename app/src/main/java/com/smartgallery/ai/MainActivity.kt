package com.smartgallery.ai

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
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
    private var btnDeleteWhatsApp: Button? = null

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            loadMediaData()
        } else {
            Toast.makeText(this, "Permissions deny hone par media load nahi hoga!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = MediaRepository(this)
        recyclerView = findViewById(R.id.recyclerViewMedia)
        btnDeleteWhatsApp = findViewById(R.id.btnDeleteWhatsApp)

        recyclerView.layoutManager = GridLayoutManager(this, 3)

        mediaAdapter = MediaAdapter(emptyList()) { item ->
            if (item.isVideo) {
                openFullScreenVideo(item.uri)
            } else {
                openFullScreenImage(item.uri)
            }
        }
        recyclerView.adapter = mediaAdapter

        btnDeleteWhatsApp?.setOnClickListener {
            confirmAndCleanAllMedia()
        }

        checkAndRequestAllPermissions()
    }

    private fun openFullScreenImage(imageUri: Uri) {
        val builder = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        Glide.with(this).load(imageUri).into(imageView)

        builder.setView(imageView)
        val dialog = builder.create()
        imageView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun openFullScreenVideo(videoUri: Uri) {
        val builder = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val videoView = VideoView(this)
        videoView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)
        videoView.setVideoURI(videoUri)

        builder.setView(videoView)
        val dialog = builder.create()

        dialog.setOnDismissListener {
            videoView.stopPlayback()
        }

        dialog.show()
        videoView.start()
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

    private fun confirmAndCleanAllMedia() {
        AlertDialog.Builder(this)
            .setTitle("Smart Clean Engine")
            .setMessage("Kya aap WhatsApp Media, Screenshots aur Unwanted Data saaf karna chahte hain?")
            .setPositiveButton("Delete All") { _, _ ->
                executeDeepClean()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeDeepClean() {
        CoroutineScope(Dispatchers.IO).launch {
            var totalDeleted = 0

            totalDeleted += deleteFromUri(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Images.Media.DATA} LIKE ? OR ${MediaStore.Images.Media.DATA} LIKE ?",
                arrayOf("%WhatsApp%", "%Screenshot%")
            )

            totalDeleted += deleteFromUri(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Video.Media.DATA} LIKE ?",
                arrayOf("%WhatsApp%")
            )

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "$totalDeleted items clean kar diye gaye hain!", Toast.LENGTH_LONG).show()
                loadMediaData()
            }
        }
    }

    private fun deleteFromUri(contentUri: Uri, selection: String, selectionArgs: Array<String>): Int {
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
