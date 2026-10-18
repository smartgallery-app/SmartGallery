package com.smartgallery.app.ui

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
import android.view.ScaleGestureDetector
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartgallery.app.MediaItem
import com.smartgallery.app.R
import com.smartgallery.app.ai.SecretAiScannerService
import com.smartgallery.app.trash.TrashActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AlbumItem(val folderName: String, val coverUri: String, val itemCount: Int)

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryAdapter
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private val mediaList = ArrayList<MediaItem>()
    private val albumList = ArrayList<AlbumItem>()
    private var currentGridSpanCount = 3
    private var isAlbumView = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, currentGridSpanCount)

        adapter = GalleryAdapter(this, mediaList) { mediaItem ->
            val intent = Intent(this, FullScreenActivity::class.java).apply {
                putExtra("media_uri", mediaItem.uriString)
                putExtra("is_video", mediaItem.isVideo)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleEnd(detector: ScaleGestureDetector) {
                if (detector.scaleFactor < 0.85f && currentGridSpanCount < 5) {
                    currentGridSpanCount++
                    updateGridSpan()
                } else if (detector.scaleFactor > 1.15f && currentGridSpanCount > 2) {
                    currentGridSpanCount--
                    updateGridSpan()
                }
            }
        })

        recyclerView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            false
        }

        bottomNav = findViewById(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_photos -> {
                    isAlbumView = false
                    recyclerView.layoutManager = GridLayoutManager(this, currentGridSpanCount)
                    loadMedia()
                    true
                }
                R.id.nav_albums -> {
                    isAlbumView = true
                    recyclerView.layoutManager = GridLayoutManager(this, 2)
                    loadAlbums()
                    true
                }
                else -> false
            }
        }

        checkPermissionsAndLoad()
    }

    private fun updateGridSpan() {
        if (!isAlbumView) {
            (recyclerView.layoutManager as GridLayoutManager).spanCount = currentGridSpanCount
            adapter.notifyItemRangeChanged(0, mediaList.size)
        }
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
            val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DATE_ADDED)
            val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

            contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)?.use { cursor ->
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

            contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)?.use { cursor ->
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

    private fun loadAlbums() {
        CoroutineScope(Dispatchers.IO).launch {
            val albumMap = HashMap<String, ArrayList<String>>()
            for (item in mediaList) {
                val folderName = File(item.path).parentFile?.name ?: "Others"
                if (!albumMap.containsKey(folderName)) albumMap[folderName] = ArrayList()
                albumMap[folderName]?.add(item.uriString)
            }
            val tempAlbums = ArrayList<AlbumItem>()
            for ((name, uris) in albumMap) {
                if (uris.isNotEmpty()) tempAlbums.add(AlbumItem(name, uris[0], uris.size))
            }
            withContext(Dispatchers.Main) {
                albumList.clear()
                albumList.addAll(tempAlbums)
                supportActionBar?.title = "Albums (${albumList.size})"
            }
        }
    }

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
            R.id.action_trash -> { startActivity(Intent(this, TrashActivity::class.java)); true }
            R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
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
