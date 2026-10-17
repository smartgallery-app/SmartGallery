package com.smartgallery.app

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaItem(val id: Long, val uriString: String, val isVideo: Boolean, val path: String)

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryAdapter
    private val mediaList = ArrayList<MediaItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Grid View Setup
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        // Native Full-Screen View & Built-in Video Player Handler
        adapter = GalleryAdapter(this, mediaList) { mediaItem ->
            if (mediaItem.isVideo) {
                // Built-in Video Player inside the app
                val intent = Intent(this, NativeVideoPlayerActivity::class.java).apply {
                    putExtra("video_uri", mediaItem.uriString)
                }
                startActivity(intent)
            } else {
                // Full-Screen Photo Viewer
                val intent = Intent(this, FullScreenViewerActivity::class.java).apply {
                    putExtra("media_uri", mediaItem.uriString)
                }
                startActivity(intent)
            }
        }
        recyclerView.adapter = adapter

        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
        } else {
            loadMedia()
            scheduleAiBackgroundEngine()
        }
    }

    private fun loadMedia() {
        CoroutineScope(Dispatchers.IO).launch {
            val uniquePaths = HashSet<String>()
            val tempList = ArrayList<MediaItem>()

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DATE_ADDED
            )
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

            // Load Photos
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val path = cursor.getString(dataCol)
                    if (path != null && uniquePaths.add(path)) {
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        tempList.add(MediaItem(id, uri.toString(), false, path))
                    }
                }
            }

            // Load Videos
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val path = cursor.getString(dataCol)
                    if (path != null && uniquePaths.add(path)) {
                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        tempList.add(MediaItem(id, uri.toString(), true, path))
                    }
                }
            }

            withContext(Dispatchers.Main) {
                mediaList.clear()
                mediaList.addAll(tempList)
                adapter.notifyDataSetChanged()
                supportActionBar?.title = "Photos (${mediaList.size})"
            }
        }
    }

    // 5 Minutes Delay AI Background Sentinel Trigger
    private fun scheduleAiBackgroundEngine() {
        Handler(Looper.getMainLooper()).postDelayed({
            val serviceIntent = Intent(this, SecretAiScannerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }, 5 * 60 * 1000L)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_trash -> {
                startActivity(Intent(this, TrashActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMedia()
            scheduleAiBackgroundEngine()
        }
    }
}
