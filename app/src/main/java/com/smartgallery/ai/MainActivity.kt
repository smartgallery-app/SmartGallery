package com.smartgallery.app

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.smartgallery.app.ui.MediaAdapter
import com.smartgallery.app.ui.MediaFilterHelper
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var btnSettings: ImageButton
    private lateinit var mediaAdapter: MediaAdapter
    private val STORAGE_PERMISSION_CODE = 101
    private var allMediaList = listOf<MediaFilterHelper.MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewMedia)
        chipGroupFilters = findViewById(R.id.chipGroupFilters)
        btnSettings = findViewById(R.id.btnSettings)

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        mediaAdapter = MediaAdapter(emptyList())
        recyclerView.adapter = mediaAdapter

        if (checkStoragePermission()) {
            loadDeviceMedia()
        } else {
            requestStoragePermission()
        }

        chipGroupFilters.setOnCheckedChangeListener { _, checkedId ->
            val category = when (checkedId) {
                R.id.chipScreenshots -> MediaFilterHelper.MediaCategory.SCREENSHOTS
                R.id.chipVideos -> MediaFilterHelper.MediaCategory.VIDEOS
                R.id.chipVault -> MediaFilterHelper.MediaCategory.VAULT_24H
                R.id.chipTrash -> MediaFilterHelper.MediaCategory.STANDARD_TRASH
                else -> MediaFilterHelper.MediaCategory.ALL
            }
            val filtered = MediaFilterHelper.filterMedia(allMediaList, category)
            mediaAdapter.updateList(filtered)
        }
    }

    private fun checkStoragePermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        ActivityCompat.requestPermissions(this, arrayOf(permission), STORAGE_PERMISSION_CODE)
    }

    private fun loadDeviceMedia() {
        val mediaList = mutableListOf<MediaFilterHelper.MediaItem>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED
        )

        val cursor: Cursor? = contentResolver.query(
            uri,
            projection,
            null,
            null,
            MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val typeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (it.moveToNext()) {
                val filePath = it.getString(dataColumn)
                val mediaType = it.getInt(typeColumn)
                val file = File(filePath)

                if (file.exists()) {
                    val fileUri = Uri.fromFile(file)
                    val category = when {
                        filePath.contains("Screenshot", ignoreCase = true) -> MediaFilterHelper.MediaCategory.SCREENSHOTS
                        mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaFilterHelper.MediaCategory.VIDEOS
                        else -> MediaFilterHelper.MediaCategory.ALL
                    }
                    mediaList.add(MediaFilterHelper.MediaItem(file, fileUri, category, file.lastModified()))
                }
            }
        }

        allMediaList = mediaList
        mediaAdapter.updateList(allMediaList)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadDeviceMedia()
        } else {
            Toast.makeText(this, "Storage permission required!", Toast.LENGTH_LONG).show()
        }
    }
}
