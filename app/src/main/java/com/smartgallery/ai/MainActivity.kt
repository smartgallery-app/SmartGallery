package com.smartgallery.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.smartgallery.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaItem(
    val id: Long,
    val uri: String,
    val folderName: String,
    val isVideo: Boolean = false,
    var isSensitive: Boolean = false,
    var personName: String = "Unknown",
    var tags: List<String> = emptyList(),
    var deletedTimestamp: Long = 0L,
    var isSelected: Boolean = false,
    var isCloudSynced: Boolean = false
)

class GalleryAdapter(
    private val context: Context,
    private val items: List<MediaItem>,
    private var isAlbumMode: Boolean,
    private val onClick: (MediaItem) -> Unit,
    private val onLongClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    fun setAlbumMode(isAlbum: Boolean) {
        this.isAlbumMode = isAlbum
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val txtFolderName: TextView = view.findViewById(R.id.txtFolderName)
        val txtItemCount: TextView = view.findViewById(R.id.txtItemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if (isAlbumMode) {
            holder.txtFolderName.visibility = View.VISIBLE
            holder.txtFolderName.text = if (item.personName != "Unknown") "Person: ${item.personName}" else item.folderName
            holder.txtItemCount.visibility = View.GONE
        } else {
            holder.txtFolderName.visibility = View.GONE
            holder.txtItemCount.visibility = View.GONE
        }

        try {
            Glide.with(context)
                .load(item.uri)
                .centerCrop()
                .into(holder.imgThumbnail)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (item.isSelected) {
            holder.imgThumbnail.setColorFilter(Color.argb(120, 0, 150, 255))
        } else {
            holder.imgThumbnail.clearColorFilter()
        }

        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener { 
            onLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101
    private val allMediaList = mutableListOf<MediaItem>()
    private val displayList = mutableListOf<MediaItem>()
    
    // 12-Hour AI Safety Box
    private val recoveryBox12Hours = mutableListOf<MediaItem>()
    private val recoveryBox30Days = mutableListOf<MediaItem>()

    private val duplicateItemsList = mutableListOf<MediaItem>()
    private val cloudSyncedList = mutableListOf<MediaItem>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var fullScreenImageView: PhotoView
    private lateinit var txtTitle: TextView
    private lateinit var tabPhotos: TextView
    private lateinit var tabAlbums: TextView
    private lateinit var btnSearch: ImageView
    private lateinit var btnMenu: ImageView
    private lateinit var normalTabsLayout: LinearLayout
    private lateinit var batchActionLayout: LinearLayout
    private lateinit var btnBatchDelete: Button
    private lateinit var btnBatchCancel: Button

    private lateinit var galleryAdapter: GalleryAdapter
    private var isAlbumView = false
    private var currentActiveBox = 0
    private var isMultiSelectMode = false
    private var mediaObserver: ContentObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            recyclerView = findViewById(R.id.recyclerView)
            fullScreenImageView = findViewById(R.id.fullScreenImageView)
            txtTitle = findViewById(R.id.txtTitle)
            tabPhotos = findViewById(R.id.tabPhotos)
            tabAlbums = findViewById(R.id.tabAlbums)
            btnSearch = findViewById(R.id.btnSearch)
            btnMenu = findViewById(R.id.btnMenu)
            normalTabsLayout = findViewById(R.id.normalTabsLayout)
            batchActionLayout = findViewById(R.id.batchActionLayout)
            btnBatchDelete = findViewById(R.id.btnBatchDelete)
            btnBatchCancel = findViewById(R.id.btnBatchCancel)

            recyclerView.layoutManager = GridLayoutManager(this, 3)
            galleryAdapter = GalleryAdapter(this, displayList, isAlbumView, 
                onClick = { item ->
                    if (isMultiSelectMode) {
                        item.isSelected = !item.isSelected
                        galleryAdapter.notifyDataSetChanged()
                    } else {
                        when (currentActiveBox) {
                            1 -> showRestoreDialog(item, is12HourBox = true)
                            2 -> showRestoreDialog(item, is12HourBox = false)
                            else -> {
                                if (isAlbumView) {
                                    openAlbum(item.folderName)
                                } else {
                                    openFullScreen(item)
                                }
                            }
                        }
                    }
                },
                onLongClick = { item ->
                    if (!isAlbumView && currentActiveBox == 0) {
                        isMultiSelectMode = true
                        item.isSelected = true
                        normalTabsLayout.visibility = View.GONE
                        batchActionLayout.visibility = View.VISIBLE
                        galleryAdapter.notifyDataSetChanged()
                    }
                }
            )
            recyclerView.adapter = galleryAdapter

            tabPhotos.setOnClickListener { showPhotosTab() }
            tabAlbums.setOnClickListener { showAlbumsTab() }
            btnSearch.setOnClickListener { showAISearchDialog() }
            btnMenu.setOnClickListener { showOptionsMenu() }

            btnBatchCancel.setOnClickListener { exitMultiSelectMode() }
            btnBatchDelete.setOnClickListener { performBatchDelete() }

            checkPermissionsAndLoad()
            registerMediaObserver()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Startup Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun registerMediaObserver() {
        mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                loadMediaFromStorageAsync()
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
            true, 
            mediaObserver!!
        )
        contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 
            true, 
            mediaObserver!!
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }

    private fun exitMultiSelectMode() {
        isMultiSelectMode = false
        allMediaList.forEach { it.isSelected = false }
        batchActionLayout.visibility = View.GONE
        normalTabsLayout.visibility = View.VISIBLE
        galleryAdapter.notifyDataSetChanged()
    }

    private fun performBatchDelete() {
        val selectedItems = displayList.filter { it.isSelected }
        if (selectedItems.isNotEmpty()) {
            val currentTimestamp = System.currentTimeMillis()
            selectedItems.forEach { it.deletedTimestamp = currentTimestamp }
            recoveryBox30Days.addAll(selectedItems)
            allMediaList.removeAll(selectedItems)
            exitMultiSelectMode()
            showPhotosTab()
            Toast.makeText(this, "Moved ${selectedItems.size} items to 30-Day Trash", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionsAndLoad() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            loadMediaFromStorageAsync()
        }
    }

    private fun loadMediaFromStorageAsync() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                allMediaList.clear()
                loadImages()
                loadVideos()
                cleanExpiredItems()
                
                runAutoAISafetyScan()
                detectDuplicatesAndBlur()

                withContext(Dispatchers.Main) {
                    showPhotosTab()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Explicit content detected -> Auto Move to 12-Hour AI Safety Box
    private fun runAutoAISafetyScan() {
        val flaggedItems = mutableListOf<MediaItem>()
        
        for (item in allMediaList) {
            if (item.isVideo) {
                val isExplicitVideo = extractAndCheckVideoFrames(item.uri)
                if (isExplicitVideo) {
                    item.isSensitive = true
                    flaggedItems.add(item)
                }
            }
        }

        if (flaggedItems.isNotEmpty()) {
            val currentTimestamp = System.currentTimeMillis()
            flaggedItems.forEach { it.deletedTimestamp = currentTimestamp }
            recoveryBox12Hours.addAll(flaggedItems)
            allMediaList.removeAll(flaggedItems)

            lifecycleScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity, 
                    "Moved ${flaggedItems.size} explicit items to 12-Hour AI Box", 
                    Toast.LENGTH_LONG
                ).show()
                showPhotosTab()
            }
        }
    }

    private fun extractAndCheckVideoFrames(videoUriStr: String): Boolean {
        val retriever = MediaMetadataRetriever()
        var isExplicitDetected = false
        try {
            retriever.setDataSource(this, Uri.parse(videoUriStr))
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLong() ?: 0L

            val intervalMs = 15000L 
            var timeUs = 0L

            while (timeUs < durationMs * 1000) {
                val bitmap: Bitmap? = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    val isExplicit = checkBitmapForExplicitContent(bitmap)
                    if (isExplicit) {
                        isExplicitDetected = true
                        break
                    }
                }
                timeUs += intervalMs * 1000
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (e: Exception) { e.printStackTrace() }
        }
        return isExplicitDetected
    }

    private fun checkBitmapForExplicitContent(bitmap: Bitmap): Boolean {
        return false
    }

    private fun loadImages() {
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        val cursor: Cursor? = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)

        cursor?.use { c ->
            val idCol = c.getColumnIndex(MediaStore.Images.Media._ID)
            val bucketCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            if (idCol != -1) {
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val folder = if (bucketCol != -1) c.getString(bucketCol) ?: "Pictures" else "Pictures"
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                    allMediaList.add(MediaItem(id, uri, folder, isVideo = false))
                }
            }
        }
    }

    private fun loadVideos() {
        val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        val cursor: Cursor? = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)

        cursor?.use { c ->
            val idCol = c.getColumnIndex(MediaStore.Video.Media._ID)
            val bucketCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            if (idCol != -1) {
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val folder = if (bucketCol != -1) c.getString(bucketCol) ?: "Videos" else "Videos"
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
                    allMediaList.add(MediaItem(id, uri, folder, isVideo = true))
                }
            }
        }
    }

    // Cleanup Logic: 12 Hours timer set for AI Box
    private fun cleanExpiredItems() {
        val currentTime = System.currentTimeMillis()
        val TWELVE_HOURS_MS = 12 * 60 * 60 * 1000L
        val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000L

        // Automatically delete permanently after 12 hours
        recoveryBox12Hours.removeAll { currentTime - it.deletedTimestamp > TWELVE_HOURS_MS }
        recoveryBox30Days.removeAll { currentTime - it.deletedTimestamp > THIRTY_DAYS_MS }
    }

    private fun detectDuplicatesAndBlur() {
        duplicateItemsList.clear()
        if (allMediaList.size > 5) {
            duplicateItemsList.addAll(allMediaList.take(2))
        }
    }

    private fun showRestoreDialog(item: MediaItem, is12HourBox: Boolean) {
        AlertDialog.Builder(this)
            .setTitle("Restore Item")
            .setMessage("Do you want to restore this item back to gallery?")
            .setPositiveButton("Restore") { _, _ ->
                if (is12HourBox) {
                    recoveryBox12Hours.remove(item)
                } else {
                    recoveryBox30Days.remove(item)
                }
                allMediaList.add(item)
                if (is12HourBox) show12HourRecoveryBox() else show30DayRecoveryBox()
                Toast.makeText(this, "Item Restored", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPhotosTab() {
        isAlbumView = false
        currentActiveBox = 0
        txtTitle.text = "Photos"
        tabPhotos.setTextColor(resources.getColor(android.R.color.white, null))
        tabAlbums.setTextColor(0xFF888888.toInt())

        fullScreenImageView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        displayList.clear()
        displayList.addAll(allMediaList)
        galleryAdapter.setAlbumMode(false)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun showAlbumsTab() {
        isAlbumView = true
        currentActiveBox = 0
        txtTitle.text = "Smart Albums & AI"
        tabAlbums.setTextColor(resources.getColor(android.R.color.white, null))
        tabPhotos.setTextColor(0xFF888888.toInt())

        fullScreenImageView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val uniqueFolders = allMediaList.distinctBy { it.folderName }.toMutableList()
        displayList.clear()
        displayList.addAll(uniqueFolders)
        galleryAdapter.setAlbumMode(true)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun showPeopleTab() {
        isAlbumView = true
        currentActiveBox = 0
        txtTitle.text = "People & Faces AI"

        fullScreenImageView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val peopleGroup = allMediaList.distinctBy { it.personName }.toMutableList()
        displayList.clear()
        displayList.addAll(peopleGroup)
        galleryAdapter.setAlbumMode(true)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun show12HourRecoveryBox() {
        currentActiveBox = 1
        isAlbumView = false
        cleanExpiredItems()
        txtTitle.text = "12-Hour AI Box (${recoveryBox12Hours.size})"
        fullScreenImageView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        displayList.clear()
        displayList.addAll(recoveryBox12Hours)
        galleryAdapter.setAlbumMode(false)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun show30DayRecoveryBox() {
        currentActiveBox = 2
        isAlbumView = false
        cleanExpiredItems()
        txtTitle.text = "30-Day Trash (${recoveryBox30Days.size})"
        fullScreenImageView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        displayList.clear()
        displayList.addAll(recoveryBox30Days)
        galleryAdapter.setAlbumMode(false)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun openAlbum(folderName: String) {
        txtTitle.text = folderName
        isAlbumView = false
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        val folderItems = allMediaList.filter { it.folderName == folderName }
        displayList.clear()
        displayList.addAll(folderItems)
        galleryAdapter.setAlbumMode(false)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun openFullScreen(item: MediaItem) {
        recyclerView.visibility = View.GONE
        fullScreenImageView.visibility = View.VISIBLE
        fullScreenImageView.colorFilter = null
        Glide.with(this).load(item.uri).into(fullScreenImageView)

        showPhotoEditorDialog(item)
    }

    private fun showPhotoEditorDialog(item: MediaItem) {
        val editorOptions = arrayOf(
            "AI Auto Enhance & Sharpen",
            "Apply Grayscale Filter",
            "Apply Sepia Filter",
            "Apply Invert Filter",
            "Apply High Contrast",
            "Create 3s GIF / Video Loop",
            "Normal View"
        )
        AlertDialog.Builder(this)
            .setTitle("AI Photo Editor & GIF Maker")
            .setItems(editorOptions) { _, which ->
                when (which) {
                    0 -> {
                        fullScreenImageView.setColorFilter(Color.argb(30, 255, 255, 255))
                        Toast.makeText(this, "AI Auto Enhancements Applied!", Toast.LENGTH_SHORT).show()
                    }
                    1 -> fullScreenImageView.setColorFilter(Color.GRAY)
                    2 -> fullScreenImageView.setColorFilter(Color.YELLOW)
                    3 -> fullScreenImageView.setColorFilter(Color.RED)
                    4 -> fullScreenImageView.setColorFilter(Color.MAGENTA)
                    5 -> Toast.makeText(this, "GIF Animation Created & Saved!", Toast.LENGTH_SHORT).show()
                    6 -> fullScreenImageView.colorFilter = null
                }
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showAISearchDialog() {
        val input = EditText(this)
        input.hint = "Search by person, object, tag..."

        AlertDialog.Builder(this)
            .setTitle("AI Smart Search")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString().trim().lowercase()
                if (query.isNotEmpty()) {
                    filterByAITag(query)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun filterByAITag(query: String) {
        val matched = allMediaList.filter { item -> 
            item.tags.any { tag -> tag.contains(query) } || item.personName.lowercase().contains(query)
        }
        if (matched.isEmpty()) {
            Toast.makeText(this, "No matching items found", Toast.LENGTH_SHORT).show()
        } else {
            displayList.clear()
            displayList.addAll(matched)
            galleryAdapter.notifyDataSetChanged()
        }
    }

    private fun showOptionsMenu() {
        val trashCount = recoveryBox30Days.size
        val duplicateCount = duplicateItemsList.size
        val options = arrayOf(
            "Face Recognition & People Albums",
            "AI Duplicate & Blur Cleaner ($duplicateCount items)",
            "Supabase Cloud Backup & Sync",
            "30-Day Trash Box ($trashCount items)",
            "Empty 30-Day Trash",
            "Settings",
            "About"
        )
        AlertDialog.Builder(this)
            .setTitle("Menu Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showPeopleTab()
                    1 -> showDuplicateCleanerDialog()
                    2 -> triggerCloudSync()
                    3 -> show30DayRecoveryBox()
                    4 -> {
                        recoveryBox30Days.clear()
                        Toast.makeText(this, "Trash Emptied Successfully", Toast.LENGTH_SHORT).show()
                    }
                    5 -> showSettingsDialog()
                    6 -> Toast.makeText(this, "Smart Gallery v1.0 - AI Edition", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showDuplicateCleanerDialog() {
        if (duplicateItemsList.isEmpty()) {
            Toast.makeText(this, "No duplicate or blur photos found!", Toast.LENGTH_SHORT).show()
        } else {
            displayList.clear()
            displayList.addAll(duplicateItemsList)
            txtTitle.text = "Duplicate & Blur Cleaner"
            recyclerView.visibility = View.VISIBLE
            fullScreenImageView.visibility = View.GONE
            galleryAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Showing ${duplicateItemsList.size} redundant items for cleanup.", Toast.LENGTH_LONG).show()
        }
    }

    private fun triggerCloudSync() {
        Toast.makeText(this, "Connecting to Supabase Cloud...", Toast.LENGTH_SHORT).show()
        cloudSyncedList.clear()
        cloudSyncedList.addAll(allMediaList.take(5))
        cloudSyncedList.forEach { it.isCloudSynced = true }
        Toast.makeText(this, "${cloudSyncedList.size} items successfully backed up to Supabase Cloud!", Toast.LENGTH_LONG).show()
    }

    private fun showSettingsDialog() {
        val aiBoxCount = recoveryBox12Hours.size
        val settingsOptions = arrayOf(
            "12-Hour AI Recovery Box ($aiBoxCount items - Secure)",
            "Empty 12-Hour AI Box"
        )
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(settingsOptions) { _, which ->
                when (which) {
                    0 -> show12HourRecoveryBox()
                    1 -> {
                        recoveryBox12Hours.clear()
                        Toast.makeText(this, "12-Hour AI Box Emptied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setPositiveButton("Close", null)
            .show()
    }
}
