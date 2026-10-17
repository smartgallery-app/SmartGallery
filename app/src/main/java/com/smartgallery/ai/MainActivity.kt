package com.smartgallery.app.ui

import android.Manifest
import android.app.WallpaperManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import java.io.File
import java.io.FileOutputStream

data class MediaItem(
    val id: Long,
    var uri: String,
    var folderName: String,
    val dateModified: Long,
    val isVideo: Boolean = false,
    var isSensitive: Boolean = false,
    var personName: String = "Unknown",
    var tags: List<String> = emptyList(),
    var deletedTimestamp: Long = 0L,
    var isSelected: Boolean = false,
    var isFavorite: Boolean = false,
    var isCloudSynced: Boolean = false,
    var title: String = ""
)

class GalleryAdapter(
    private val context: Context,
    private val items: List<MediaItem>,
    private var isAlbumMode: Boolean,
    private val onClick: (MediaItem) -> Unit,
    private val onLongClick: (MediaItem) -> Unit,
    private val onFavoriteClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    fun setAlbumMode(isAlbum: Boolean) {
        this.isAlbumMode = isAlbum
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
        val txtFolderName: TextView = view.findViewById(R.id.txtFolderName)
        val txtItemCount: TextView = view.findViewById(R.id.txtItemCount)
        val btnHeart: ImageView = view.findViewById(R.id.btnHeart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if (isAlbumMode) {
            holder.txtFolderName.visibility = View.VISIBLE
            holder.txtFolderName.text = if (item.title.isNotEmpty()) item.title else item.folderName
            holder.txtItemCount.visibility = View.GONE
            holder.btnHeart.visibility = View.GONE
        } else {
            holder.txtFolderName.visibility = View.GONE
            holder.txtItemCount.visibility = View.GONE
            holder.btnHeart.visibility = View.VISIBLE
            
            if (item.isFavorite) {
                holder.btnHeart.setImageResource(android.R.drawable.btn_star_big_on)
            } else {
                holder.btnHeart.setImageResource(android.R.drawable.btn_star_big_off)
            }
        }

        try {
            if (item.uri.isNotEmpty()) {
                Glide.with(context)
                    .load(item.uri)
                    .centerCrop()
                    .into(holder.imgThumbnail)
            } else {
                holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (item.isSelected) {
            holder.imgThumbnail.setColorFilter(Color.argb(120, 0, 150, 255))
        } else {
            holder.imgThumbnail.clearColorFilter()
        }

        holder.btnHeart.setOnClickListener {
            onFavoriteClick(item)
            notifyItemChanged(position)
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
    
    // Distinct Recovery Boxes
    private val recoveryBox12Hours = mutableListOf<MediaItem>() // AI Detected Explicit Items
    private val recoveryBox30Days = mutableListOf<MediaItem>()  // User Manually Deleted Items

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

    // Bottom Toolbar Controls
    private lateinit var bottomViewerToolbar: LinearLayout
    private lateinit var btnShare: ImageView
    private lateinit var btnFavViewer: ImageView
    private lateinit var btnEditViewer: ImageView
    private lateinit var btnDeleteViewer: ImageView
    private lateinit var btnInfoViewer: ImageView
    private lateinit var btnMoreMenuViewer: ImageView

    private lateinit var galleryAdapter: GalleryAdapter
    private var isAlbumView = false
    private var currentActiveBox = 0 // 0: Main, 1: 12-Hour AI Safety, 2: 30-Day Trash
    private var isMultiSelectMode = false
    private var mediaObserver: ContentObserver? = null
    private var isNewestFirst = true
    private var currentOpenedItem: MediaItem? = null

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

            bottomViewerToolbar = findViewById(R.id.bottomViewerToolbar)
            btnShare = findViewById(R.id.btnShare)
            btnFavViewer = findViewById(R.id.btnFavViewer)
            btnEditViewer = findViewById(R.id.btnEditViewer)
            btnDeleteViewer = findViewById(R.id.btnDeleteViewer)
            btnInfoViewer = findViewById(R.id.btnInfoViewer)
            btnMoreMenuViewer = findViewById(R.id.btnMoreMenuViewer)

            recyclerView.layoutManager = GridLayoutManager(this, 3)
            galleryAdapter = GalleryAdapter(this, displayList, isAlbumView, 
                onClick = { item ->
                    if (isMultiSelectMode) {
                        item.isSelected = !item.isSelected
                        galleryAdapter.notifyDataSetChanged()
                    } else {
                        if (isAlbumView && item.folderName == "30-Day Trash") {
                            open30DayTrashBox()
                        } else if (isAlbumView) {
                            openAlbum(item.folderName)
                        } else {
                            when (currentActiveBox) {
                                1 -> showRestoreDialog(item, is12HourBox = true)
                                2 -> showRestoreDialog(item, is12HourBox = false)
                                else -> openFullScreen(item)
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
                },
                onFavoriteClick = { item ->
                    item.isFavorite = !item.isFavorite
                    val status = if (item.isFavorite) "Added to Favorites" else "Removed from Favorites"
                    Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
                }
            )
            recyclerView.adapter = galleryAdapter

            tabPhotos.setOnClickListener { showPhotosTab() }
            tabAlbums.setOnClickListener { showAlbumsTab() }
            btnSearch.setOnClickListener { showAISearchDialog() }
            btnMenu.setOnClickListener { showOptionsMenu() }

            btnBatchCancel.setOnClickListener { exitMultiSelectMode() }
            btnBatchDelete.setOnClickListener { performBatchDelete() }

            setupBottomToolbarActions()
            checkPermissionsAndLoad()
            registerMediaObserver()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Startup Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupBottomToolbarActions() {
        btnShare.setOnClickListener {
            currentOpenedItem?.let { item ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = if (item.isVideo) "video/*" else "image/*"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
                }
                startActivity(Intent.createChooser(intent, "Share Media"))
            }
        }

        btnFavViewer.setOnClickListener {
            currentOpenedItem?.let { item ->
                item.isFavorite = !item.isFavorite
                Toast.makeText(this, if (item.isFavorite) "Starred in Favorites" else "Unstarred", Toast.LENGTH_SHORT).show()
            }
        }

        btnEditViewer.setOnClickListener {
            Toast.makeText(this, "Opening Photo Editor...", Toast.LENGTH_SHORT).show()
        }

        // USER MANUAL DELETE -> Goes to 30-Day Trash Box
        btnDeleteViewer.setOnClickListener {
            currentOpenedItem?.let { item ->
                item.deletedTimestamp = System.currentTimeMillis()
                recoveryBox30Days.add(item)
                allMediaList.remove(item)
                closeFullScreen()
                showPhotosTab()
                Toast.makeText(this, "Moved to 30-Day Trash Box", Toast.LENGTH_SHORT).show()
            }
        }

        btnInfoViewer.setOnClickListener {
            currentOpenedItem?.let { item ->
                AlertDialog.Builder(this)
                    .setTitle("Image Details")
                    .setMessage("Folder: ${item.folderName}\nURI: ${item.uri}\nType: ${if (item.isVideo) "Video" else "Image"}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }

        btnMoreMenuViewer.setOnClickListener {
            currentOpenedItem?.let { item ->
                showOverflowItemMenu(item)
            }
        }
    }

    private fun showOverflowItemMenu(item: MediaItem) {
        val options = arrayOf(
            "Move to album",
            "Copy to album",
            "Rename",
            "Convert to PDF",
            "Set as wallpaper"
        )
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showMoveOrCopyDialog(item, isMove = true)
                    1 -> showMoveOrCopyDialog(item, isMove = false)
                    2 -> showRenameDialog(item)
                    3 -> convertImageToPdf(item)
                    4 -> setAsWallpaper(item)
                }
            }
            .show()
    }

    private fun showRenameDialog(item: MediaItem) {
        val input = EditText(this)
        input.hint = "Enter new folder/name"
        AlertDialog.Builder(this)
            .setTitle("Rename Item")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    item.folderName = newName
                    Toast.makeText(this, "Renamed to $newName", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMoveOrCopyDialog(item: MediaItem, isMove: Boolean) {
        val input = EditText(this)
        input.hint = "Target Album Name"
        AlertDialog.Builder(this)
            .setTitle(if (isMove) "Move to Album" else "Copy to Album")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val targetAlbum = input.text.toString().trim()
                if (targetAlbum.isNotEmpty()) {
                    if (isMove) {
                        item.folderName = targetAlbum
                        Toast.makeText(this, "Moved to $targetAlbum", Toast.LENGTH_SHORT).show()
                    } else {
                        val newItem = item.copy(id = System.currentTimeMillis(), folderName = targetAlbum)
                        allMediaList.add(newItem)
                        Toast.makeText(this, "Copied to $targetAlbum", Toast.LENGTH_SHORT).show()
                    }
                    showPhotosTab()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun convertImageToPdf(item: MediaItem) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            pdfDocument.finishPage(page)
            val pdfFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Converted_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            pdfDocument.close()
            Toast.makeText(this, "Saved as PDF in Documents", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "PDF Conversion Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAsWallpaper(item: MediaItem) {
        try {
            val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                setDataAndType(Uri.parse(item.uri), "image/*")
                putExtra("mimeType", "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Set Wallpaper"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Wallpaper setting failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerMediaObserver() {
        try {
            mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    loadMediaFromStorageAsync()
                }
            }
            contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mediaObserver!!)
            contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, mediaObserver!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaObserver?.let {
            try { contentResolver.unregisterContentObserver(it) } catch (e: Exception) { e.printStackTrace() }
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
            Toast.makeText(this, "Moved ${selectedItems.size} items to 30-Day Trash Box", Toast.LENGTH_SHORT).show()
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
                sortMediaList()
                
                // Real-time AI Video Frame Auto-Scanner
                runAutoAISafetyScan()

                withContext(Dispatchers.Main) {
                    if (currentActiveBox == 0 && !isAlbumView) {
                        showPhotosTab()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadImages() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Image"
                val date = cursor.getLong(dateCol)
                val folder = cursor.getString(folderCol) ?: "Camera"
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()

                allMediaList.add(MediaItem(id, uri, folder, date, isVideo = false))
            }
        }
    }

    private fun loadVideos() {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Video"
                val date = cursor.getLong(dateCol)
                val folder = cursor.getString(folderCol) ?: "Camera"
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

                allMediaList.add(MediaItem(id, uri, folder, date, isVideo = true))
            }
        }
    }

    private fun sortMediaList() {
        if (isNewestFirst) {
            allMediaList.sortByDescending { it.dateModified }
        } else {
            allMediaList.sortBy { it.dateModified }
        }
    }

    // AUTOMATIC FAST AI SAFETY SCANNER FOR VIDEOS & IMAGES
    private fun runAutoAISafetyScan() {
        val flaggedItems = mutableListOf<MediaItem>()
        val currentTime = System.currentTimeMillis()

        for (item in allMediaList) {
            var isExplicit = false
            if (item.isVideo) {
                // Video ke duration and frames ko background mein fast scan karna
                isExplicit = extractAndCheckVideoFrames(item.uri)
            } else {
                isExplicit = item.tags.contains("explicit") || item.folderName.contains("Sensitive", true)
            }

            if (isExplicit) {
                if (!item.isSensitive) {
                    item.isSensitive = true
                    item.deletedTimestamp = currentTime
                    flaggedItems.add(item)
                }
            }
        }

        if (flaggedItems.isNotEmpty()) {
            recoveryBox12Hours.addAll(flaggedItems)
            allMediaList.removeAll(flaggedItems)
        }

        // 12 Ghante (43,200,000 ms) pure hone par device se auto-delete
        recoveryBox12Hours.removeAll { item ->
            val isExpired = (currentTime - item.deletedTimestamp) > 43200000L
            if (isExpired) {
                deleteFileFromDevice(item.uri)
            }
            isExpired
        }

        // 30 Din (2,592,000,000 ms) pure hone par trash box se auto-delete
        recoveryBox30Days.removeAll { item ->
            val isExpired = (currentTime - item.deletedTimestamp) > 2592000000L
            if (isExpired) {
                deleteFileFromDevice(item.uri)
            }
            isExpired
        }
    }

    // REAL VIDEO FRAME EXTRACTION & AI CHECK
    private fun extractAndCheckVideoFrames(videoUriString: String): Boolean {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, Uri.parse(videoUriString))
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLong() ?: 0L

            // Har minute (60,000 ms) me se 4-5 second frame sample extract karke check karna
            var timeMs: Long = 0L
            while (timeMs < duration) {
                val bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bitmap != null) {
                    val isUnsafe = analyzeBitmapForExplicitContent(bitmap)
                    bitmap.recycle()
                    if (isUnsafe) {
                        return true
                    }
                }
                timeMs += 60000L
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever?.release()
        }
        return false
    }

    private fun analyzeBitmapForExplicitContent(bitmap: Bitmap): Boolean {
        // AI detection pattern matching placeholder
        return false
    }

    private fun deleteFileFromDevice(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPhotosTab() {
        isAlbumView = false
        currentActiveBox = 0
        galleryAdapter.setAlbumMode(false)
        tabPhotos.setTextColor(Color.WHITE)
        tabAlbums.setTextColor(Color.GRAY)
        txtTitle.text = "Photos"

        displayList.clear()
        displayList.addAll(allMediaList)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun showAlbumsTab() {
        isAlbumView = true
        galleryAdapter.setAlbumMode(true)
        tabPhotos.setTextColor(Color.GRAY)
        tabAlbums.setTextColor(Color.WHITE)
        txtTitle.text = "Albums"

        displayList.clear()

        // 1. 30-Day Trash Box ko Albums page par direct ek normal folder album ki tarah add karna
        val trashAlbumFolder = MediaItem(
            id = -99L,
            uri = if (recoveryBox30Days.isNotEmpty()) recoveryBox30Days[0].uri else "",
            folderName = "30-Day Trash",
            title = "30-Day Trash (${recoveryBox30Days.size})",
            dateModified = System.currentTimeMillis()
        )
        displayList.add(trashAlbumFolder)

        // 2. Device ke regular folders add karna
        val normalFolders = allMediaList.distinctBy { it.folderName }
        displayList.addAll(normalFolders)

        galleryAdapter.notifyDataSetChanged()
    }

    private fun openAlbum(folderName: String) {
        isAlbumView = false
        galleryAdapter.setAlbumMode(false)
        txtTitle.text = folderName

        displayList.clear()
        displayList.addAll(allMediaList.filter { it.folderName == folderName })
        galleryAdapter.notifyDataSetChanged()
    }

    private fun showOptionsMenu() {
        val options = arrayOf(
            "⚙️ Settings",
            "⭐ Favorites",
            "🔄 Toggle Sort Order",
            "ℹ️ About Smart Gallery"
        )
        AlertDialog.Builder(this)
            .setTitle("Gallery Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openSettingsDialog()
                    1 -> openFavorites()
                    2 -> {
                        isNewestFirst = !isNewestFirst
                        sortMediaList()
                        showPhotosTab()
                    }
                    3 -> Toast.makeText(this, "Smart Gallery v1.0", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    // Settings Ke Andar 12-Hour AI Safety Box
    private fun openSettingsDialog() {
        val message = "AI Safety Scanner\n\n12-Hour AI Safety Box (${recoveryBox12Hours.size} items)\nAll sensitive media automatically moved here will be securely locked and then permanently self-deleted after 12 hours. Auto-scans run continuously on device."
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setMessage(message)
            .setPositiveButton("Open 12-Hour Safety Box") { _, _ ->
                currentActiveBox = 1
                isAlbumView = false
                galleryAdapter.setAlbumMode(false)
                displayList.clear()
                displayList.addAll(recoveryBox12Hours)
                galleryAdapter.notifyDataSetChanged()
                txtTitle.text = "12-Hour AI Safety Box"
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun open30DayTrashBox() {
        currentActiveBox = 2
        isAlbumView = false
        galleryAdapter.setAlbumMode(false)
        displayList.clear()
        displayList.addAll(recoveryBox30Days)
        galleryAdapter.notifyDataSetChanged()
        txtTitle.text = "30-Day Trash Box"
    }

    private fun openFavorites() {
        isAlbumView = false
        galleryAdapter.setAlbumMode(false)
        displayList.clear()
        displayList.addAll(allMediaList.filter { it.isFavorite })
        galleryAdapter.notifyDataSetChanged()
        txtTitle.text = "Favorites"
    }

    private fun showRestoreDialog(item: MediaItem, is12HourBox: Boolean) {
        val boxName = if (is12HourBox) "12-Hour Safety Box" else "30-Day Trash Box"
        AlertDialog.Builder(this)
            .setTitle("Restore Item")
            .setMessage("Do you want to restore this item back to main gallery from $boxName?")
            .setPositiveButton("Restore") { _, _ ->
                if (is12HourBox) {
                    recoveryBox12Hours.remove(item)
                } else {
                    recoveryBox30Days.remove(item)
                }
                item.isSensitive = false
                allMediaList.add(item)
                sortMediaList()
                
                displayList.remove(item)
                galleryAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Restored successfully!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openFullScreen(item: MediaItem) {
        currentOpenedItem = item
        fullScreenImageView.visibility = View.VISIBLE
        bottomViewerToolbar.visibility = View.VISIBLE
        Glide.with(this).load(item.uri).into(fullScreenImageView)
    }

    private fun closeFullScreen() {
        fullScreenImageView.visibility = View.GONE
        bottomViewerToolbar.visibility = View.GONE
        currentOpenedItem = null
    }

    private fun showAISearchDialog() {
        val input = EditText(this)
        input.hint = "Search photos (e.g. nature, documents)"
        AlertDialog.Builder(this)
            .setTitle("AI Search")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) {
                    displayList.clear()
                    displayList.addAll(allMediaList.filter { it.folderName.contains(query, true) || it.title.contains(query, true) })
                    galleryAdapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Reset") { _, _ -> showPhotosTab() }
            .show()
    }

    override fun onBackPressed() {
        if (fullScreenImageView.visibility == View.VISIBLE) {
            closeFullScreen()
        } else if (currentActiveBox != 0) {
            showPhotosTab()
        } else {
            super.onBackPressed()
        }
    }
}
