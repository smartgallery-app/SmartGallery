package com.smartgallery.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.smartgallery.app.R

data class MediaItem(
    val id: Long,
    val uri: String,
    val folderName: String,
    val isVideo: Boolean = false,
    var isSensitive: Boolean = false,
    var tags: List<String> = emptyList(),
    var deletedTimestamp: Long = 0L // Deletion Time Tracking
)

class GalleryAdapter(
    private val context: Context,
    private val items: List<MediaItem>,
    private var isAlbumMode: Boolean,
    private val onClick: (MediaItem) -> Unit
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
            holder.txtFolderName.text = item.folderName
            holder.txtItemCount.visibility = View.GONE
        } else {
            holder.txtFolderName.visibility = View.GONE
            holder.txtItemCount.visibility = View.GONE
        }

        Glide.with(context)
            .load(item.uri)
            .centerCrop()
            .into(holder.imgThumbnail)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101
    private val allMediaList = mutableListOf<MediaItem>()
    private val displayList = mutableListOf<MediaItem>()
    
    // Dual Recovery Boxes
    private val recoveryBox24Hours = mutableListOf<MediaItem>() // 24-Hour AI Scan Box
    private val recoveryBox30Days = mutableListOf<MediaItem>()  // 30-Day Normal Trash Box

    private lateinit var recyclerView: RecyclerView
    private lateinit var fullScreenImageView: PhotoView
    private lateinit var txtTitle: TextView
    private lateinit var tabPhotos: TextView
    private lateinit var tabAlbums: TextView
    private lateinit var btnSearch: ImageView
    private lateinit var btnMenu: ImageView

    private lateinit var galleryAdapter: GalleryAdapter
    private var isAlbumView = false
    private var currentActiveBox = 0 // 0 = Main, 1 = 24h Box, 2 = 30d Box

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        fullScreenImageView = findViewById(R.id.fullScreenImageView)
        txtTitle = findViewById(R.id.txtTitle)
        tabPhotos = findViewById(R.id.tabPhotos)
        tabAlbums = findViewById(R.id.tabAlbums)
        btnSearch = findViewById(R.id.btnSearch)
        btnMenu = findViewById(R.id.btnMenu)

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        galleryAdapter = GalleryAdapter(this, displayList, isAlbumView) { item ->
            when (currentActiveBox) {
                1 -> showRestoreDialog(item, is24HourBox = true)
                2 -> showRestoreDialog(item, is24HourBox = false)
                else -> {
                    if (isAlbumView) {
                        openAlbum(item.folderName)
                    } else {
                        openFullScreen(item.uri)
                    }
                }
            }
        }
        recyclerView.adapter = galleryAdapter

        tabPhotos.setOnClickListener { showPhotosTab() }
        tabAlbums.setOnClickListener { showAlbumsTab() }

        btnSearch.setOnClickListener { showAISearchDialog() }
        btnMenu.setOnClickListener { showOptionsMenu() }

        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            loadMediaFromStorage()
        }
    }

    private fun loadMediaFromStorage() {
        allMediaList.clear()
        loadImages()
        loadVideos()
        cleanExpiredItems()
        showPhotosTab()
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

    // Auto cleanup logic based on timestamps
    private fun cleanExpiredItems() {
        val currentTime = System.currentTimeMillis()
        val ONE_DAY_MS = 24 * 60 * 60 * 1000L
        val THIRTY_DAYS_MS = 30L * ONE_DAY_MS

        // Auto delete 24h items permanently after 24 hours
        recoveryBox24Hours.removeAll { currentTime - it.deletedTimestamp > ONE_DAY_MS }

        // Auto delete 30d items permanently after 30 days
        recoveryBox30Days.removeAll { currentTime - it.deletedTimestamp > THIRTY_DAYS_MS }
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
        txtTitle.text = "Albums"
        tabAlbums.setTextColor(resources.getColor(android.R.color.white, null))
        tabPhotos.setTextColor(0xFF888888.toInt())

        fullScreenImageView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val uniqueFolders = allMediaList.distinctBy { it.folderName }
        displayList.clear()
        displayList.addAll(uniqueFolders)
        galleryAdapter.setAlbumMode(true)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun show24HourRecoveryBox() {
        currentActiveBox = 1
        isAlbumView = false
        cleanExpiredItems()
        txtTitle.text = "24-Hour Express Recovery"
        fullScreenImageView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        displayList.clear()
        displayList.addAll(recoveryBox24Hours)
        galleryAdapter.setAlbumMode(false)
        galleryAdapter.notifyDataSetChanged()

        Toast.makeText(this, "${recoveryBox24Hours.size} items (Auto-delete in 24h)", Toast.LENGTH_SHORT).show()
    }

    private fun show30DayRecoveryBox() {
        currentActiveBox = 2
        isAlbumView = false
        cleanExpiredItems()
        txtTitle.text = "30-Day Trash Box"
        fullScreenImageView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        displayList.clear()
        displayList.addAll(recoveryBox30Days)
        galleryAdapter.setAlbumMode(false)
        galleryAdapter.notifyDataSetChanged()

        Toast.makeText(this, "${recoveryBox30Days.size} items (Auto-delete in 30d)", Toast.LENGTH_SHORT).show()
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

    private fun openFullScreen(uri: String) {
        recyclerView.visibility = View.GONE
        fullScreenImageView.visibility = View.VISIBLE
        fullScreenImageView.colorFilter = null
        Glide.with(this).load(uri).into(fullScreenImageView)
    }

    private fun showAISearchDialog() {
        val input = EditText(this)
        input.hint = "Search by object, tag..."

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
        val matched = allMediaList.filter { item -> item.tags.any { tag -> tag.contains(query) } }
        if (matched.isEmpty()) {
            Toast.makeText(this, "No matching items found", Toast.LENGTH_SHORT).show()
        } else {
            displayList.clear()
            displayList.addAll(matched)
            galleryAdapter.notifyDataSetChanged()
        }
    }

    private fun showOptionsMenu() {
        val options = arrayOf(
            "Scan & Explicit Content Detection (AI)",
            "24-Hour Recovery Box (AI Deleted)",
            "30-Day Recovery Box (Normal Trash)",
            "Settings",
            "About"
        )
        AlertDialog.Builder(this)
            .setTitle("Menu Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> runAISafetyScanAndDelete()
                    1 -> show24HourRecoveryBox()
                    2 -> show30DayRecoveryBox()
                    3 -> Toast.makeText(this, "Settings Selected", Toast.LENGTH_SHORT).show()
                    4 -> Toast.makeText(this, "Smart Gallery v1.0", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun runAISafetyScanAndDelete() {
        Toast.makeText(this, "AI Scanning Started...", Toast.LENGTH_SHORT).show()

        val itemsToRemove = mutableListOf<MediaItem>()
        val currentTimestamp = System.currentTimeMillis()

        allMediaList.forEach { item ->
            val uri = Uri.parse(item.uri)
            if (item.isVideo) {
                if (scanVideoAtOneMinute(uri)) {
                    item.deletedTimestamp = currentTimestamp
                    itemsToRemove.add(item)
                }
            } else {
                AIScannerHelper.scanImageForSafetyAndTags(this, uri) { isSensitive, tags ->
                    item.tags = tags
                    if (isSensitive) {
                        item.deletedTimestamp = currentTimestamp
                        itemsToRemove.add(item)
                    }
                }
            }
        }

        if (itemsToRemove.isNotEmpty()) {
            // Sent to 24-Hour Express Box for auto-deletion
            recoveryBox24Hours.addAll(itemsToRemove)
            allMediaList.removeAll(itemsToRemove)
            Toast.makeText(this, "Moved ${itemsToRemove.size} explicit items to 24-Hour Box!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Gallery is Safe. No explicit media found.", Toast.LENGTH_SHORT).show()
        }

        showPhotosTab()
    }

    private fun showRestoreDialog(item: MediaItem, is24HourBox: Boolean) {
        val boxName = if (is24HourBox) "24-Hour Box" else "30-Day Trash"
        AlertDialog.Builder(this)
            .setTitle(boxName)
            .setMessage("Do you want to restore this item back to Gallery or Delete it now?")
            .setPositiveButton("Restore") { _, _ ->
                if (is24HourBox) recoveryBox24Hours.remove(item) else recoveryBox30Days.remove(item)
                allMediaList.add(0, item)
                if (is24HourBox) show24HourRecoveryBox() else show30DayRecoveryBox()
                Toast.makeText(this, "Item restored back to Gallery!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Delete Permanently") { _, _ ->
                if (is24HourBox) recoveryBox24Hours.remove(item) else recoveryBox30Days.remove(item)
                if (is24HourBox) show24HourRecoveryBox() else show30DayRecoveryBox()
                Toast.makeText(this, "Item deleted permanently.", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun scanVideoAtOneMinute(videoUri: Uri): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, videoUri)
            val frameBitmap = retriever.getFrameAtTime(60_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            var isExplicit = false
            if (frameBitmap != null) {
                AIScannerHelper.scanBitmapForSafety(frameBitmap) { sensitive ->
                    isExplicit = sensitive
                }
            }
            isExplicit
        } catch (e: Exception) {
            false
        } finally {
            retriever.release()
        }
    }

    override fun onBackPressed() {
        if (fullScreenImageView.visibility == View.VISIBLE) {
            fullScreenImageView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else if (currentActiveBox != 0 || (txtTitle.text != "Photos" && txtTitle.text != "Albums")) {
            showPhotosTab()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            loadMediaFromStorage()
        } else {
            Toast.makeText(this, "Storage Permission Denied!", Toast.LENGTH_SHORT).show()
        }
    }
}
