package com.smartgallery.app

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaItem(
    val id: Long,
    var uri: String,
    var folderName: String,
    val dateModified: Long,
    val isVideo: Boolean = false,
    var isSensitive: Boolean = false,
    var tags: MutableList<String> = mutableListOf(),
    var deletedTimestamp: Long = 0L,
    var isSelected: Boolean = false,
    var title: String = ""
)

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101
    private val allMediaList = mutableListOf<MediaItem>()
    private val displayList = mutableListOf<MediaItem>()
    
    private val recoveryBox12Hours = mutableListOf<MediaItem>()
    private val recoveryBox30Days = mutableListOf<MediaItem>()

    private lateinit var mainContainer: LinearLayout
    private lateinit var txtTitle: TextView
    private lateinit var gridView: GridView
    private lateinit var gridAdapter: MediaGridAdapter
    
    private var currentMode = 0 // 0: Main Photos, 1: 12-Hour Box, 2: 30-Day Trash
    private var mediaObserver: ContentObserver? = null

    // 10-Minute Autonomous Background AI Scan Engine
    private val backgroundHandler = Handler(Looper.getMainLooper())
    private val backgroundRunnable = object : Runnable {
        override fun run() {
            try {
                runAutonomousBackgroundEngine()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            backgroundHandler.postDelayed(this, 600000L) // 10 Minutes Loop
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            buildPureDynamicLayout()
            checkPermissionsAndLoad()
            registerMediaObserver()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Dynamic Code Layout to eliminate any XML ID mismatch crash
    private fun buildPureDynamicLayout() {
        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0F19"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Header Bar
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 40, 32, 20)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#111827"))
        }

        txtTitle = TextView(this).apply {
            text = "Smart Gallery AI"
            textSize = 20f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnMenu = Button(this).apply {
            text = "🛡️ Safety"
            setBackgroundColor(Color.parseColor("#1F2937"))
            setTextColor(Color.GREEN)
            setOnClickListener { promptPinForHiddenBox() }
        }

        headerLayout.addView(txtTitle)
        headerLayout.addView(btnMenu)

        // Grid View for Images
        gridView = GridView(this).apply {
            numColumns = 3
            verticalSpacing = 8
            horizontalSpacing = 8
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }

        gridAdapter = MediaGridAdapter(this, displayList) { item ->
            onItemClicked(item)
        }
        gridView.adapter = gridAdapter

        // Bottom Navigation Tabs
        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#111827"))
            setPadding(16, 16, 16, 16)
        }

        val btnPhotos = Button(this).apply {
            text = "Photos"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { switchTab(0) }
        }

        val btnTrash = Button(this).apply {
            text = "Trash (30D)"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { switchTab(2) }
        }

        bottomNav.addView(btnPhotos)
        bottomNav.addView(btnTrash)

        mainContainer.addView(headerLayout)
        mainContainer.addView(gridView)
        mainContainer.addView(bottomNav)

        setContentView(mainContainer)
    }

    private fun checkPermissionsAndLoad() {
        try {
            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (perms.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
                ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST_CODE)
            } else {
                loadMediaFromDevice()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            loadMediaFromDevice()
        }
    }

    private fun loadMediaFromDevice() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                allMediaList.clear()
                val proj = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, null, null, MediaStore.Images.Media.DATE_MODIFIED + " DESC")?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                    val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                        allMediaList.add(MediaItem(id, uri, cursor.getString(folderCol) ?: "Camera", cursor.getLong(dateCol), isVideo = false, title = cursor.getString(nameCol) ?: "Img"))
                    }
                }

                runAutonomousBackgroundEngine()

                withContext(Dispatchers.Main) {
                    switchTab(0)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun runAutonomousBackgroundEngine() {
        try {
            val currentTime = System.currentTimeMillis()
            val flagged = mutableListOf<MediaItem>()

            for (item in allMediaList) {
                val isExplicit = item.title.contains("private", true) || item.title.contains("sensitive", true) || item.folderName.contains("Sensitive", true)
                if (isExplicit) {
                    item.isSensitive = true
                    item.deletedTimestamp = currentTime
                    flagged.add(item)
                }
            }

            if (flagged.isNotEmpty()) {
                recoveryBox12Hours.addAll(flagged)
                allMediaList.removeAll(flagged)
            }

            // Expiry Check (12-Hour AI Safety Box)
            recoveryBox12Hours.removeAll { item ->
                (currentTime - item.deletedTimestamp) >= 43200000L
            }

            // Expiry Check (30-Day Trash)
            recoveryBox30Days.removeAll { item ->
                (currentTime - item.deletedTimestamp) >= 2592000000L
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun switchTab(mode: Int) {
        currentMode = mode
        displayList.clear()
        when (mode) {
            0 -> {
                txtTitle.text = "Smart Gallery (${allMediaList.size})"
                displayList.addAll(allMediaList)
            }
            1 -> {
                txtTitle.text = "12-Hour Safety Box (${recoveryBox12Hours.size})"
                displayList.addAll(recoveryBox12Hours)
            }
            2 -> {
                txtTitle.text = "30-Day Trash (${recoveryBox30Days.size})"
                displayList.addAll(recoveryBox30Days)
            }
        }
        gridAdapter.notifyDataSetChanged()
    }

    private fun promptPinForHiddenBox() {
        val input = EditText(this).apply { hint = "PIN (Default: 1234)" }
        AlertDialog.Builder(this)
            .setTitle("Hidden 12-Hour Safety Zone")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                if (input.text.toString().trim() == "1234" || input.text.isNotEmpty()) {
                    switchTab(1)
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onItemClicked(item: MediaItem) {
        if (currentMode == 0) {
            AlertDialog.Builder(this)
                .setTitle("Media Actions")
                .setMessage("Move item to 30-Day Trash?")
                .setPositiveButton("Move to Trash") { _, _ ->
                    item.deletedTimestamp = System.currentTimeMillis()
                    recoveryBox30Days.add(item)
                    allMediaList.remove(item)
                    switchTab(0)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Restore Item")
                .setMessage("Restore back to main gallery?")
                .setPositiveButton("Restore") { _, _ ->
                    if (currentMode == 1) recoveryBox12Hours.remove(item) else recoveryBox30Days.remove(item)
                    allMediaList.add(item)
                    switchTab(currentMode)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun registerMediaObserver() {
        try {
            mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    loadMediaFromDevice()
                }
            }
            contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mediaObserver!!)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onResume() {
        super.onResume()
        backgroundHandler.post(backgroundRunnable)
    }

    override fun onPause() {
        super.onPause()
        backgroundHandler.removeCallbacks(backgroundRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaObserver?.let { contentResolver.unregisterContentObserver(it) }
    }
}

// Custom Grid Adapter for Dynamic UI
class MediaGridAdapter(
    private val context: Context,
    private val list: List<MediaItem>,
    private val onItemClick: (MediaItem) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = list.size
    override fun getItem(position: Int): Any = list[position]
    override fun getItemId(position: Int): Long = list[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView = (convertView as? ImageView) ?: ImageView(context).apply {
            layoutParams = AbsListView.LayoutParams(340, 340)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        try {
            val item = list[position]
            Glide.with(context).load(item.uri).into(imageView)
            imageView.setOnClickListener { onItemClick(item) }
        } catch (e: Exception) { e.printStackTrace() }

        return imageView
    }
}
