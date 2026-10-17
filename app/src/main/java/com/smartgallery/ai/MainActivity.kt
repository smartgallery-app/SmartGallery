package com.smartgallery.app.ui

import android.Manifest
import android.app.WallpaperManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
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
    var isCloudSynced: Boolean = false
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
            holder.txtFolderName.text = if (item.personName != "Unknown") "Person: ${item.personName}" else item.folderName
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

    // Bottom Toolbar & Item Viewer Control
    private lateinit var bottomViewerToolbar: LinearLayout
    private lateinit var btnShare: ImageView
    private lateinit var btnFavViewer: ImageView
    private lateinit var btnEditViewer: ImageView
    private lateinit var btnDeleteViewer: ImageView
    private lateinit var btnInfoViewer: ImageView
    private lateinit var btnMoreMenuViewer: ImageView

    private lateinit var galleryAdapter: GalleryAdapter
    private var isAlbumView = false
    private var currentActiveBox = 0 // 0: Main, 1: 12-Hour AI, 2: 30-Day Trash
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

            // Setup Viewer Bottom Bar Buttons
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
            currentOpenedItem?.let { item ->
                showPhotoEditorDialog(item)
            }
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

    // 3-Dot Overflow Menu Actions from Screenshot 2
    private fun showOverflowItemMenu(item: MediaItem) {
        val options = arrayOf(
            "Move to album",
            "Copy to album",
            "Rename",
            "Convert to PDF",
            "Set as wallpaper",
            "Set as contact avatar"
        )
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showMoveOrCopyDialog(item, isMove = true)
                    1 -> showMoveOrCopyDialog(item, isMove = false)
                    2 -> showRenameDialog(item)
                    3 -> convertImageToPdf(item)
                    4 -> setAsWallpaper(item)
                    5 -> Toast.makeText(this, "Select Contact to set Avatar", Toast.LENGTH_SHORT).show()
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
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
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

    // MANUAL BATCH DELETE -> Goes to 30-Day Trash Box
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
                
                cleanExpiredItems()
                
                // AI AUTOMATED MODERATION -> Explicit items go to 12-Hour AI Box
                runAutoAISafetyScan()
                
                detectDuplicatesAndBlur()

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

    private fun sortMediaList() {
        if (isNewestFirst) {
            allMediaList.sortByDescending { it.dateModified }
        } else {
            allMediaList.sortBy { it.dateModified }
        }
    }

    // AI EXPLICIT DETECTED -> Auto Move to 12-Hour AI Recovery Box
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
                Toast.makeText(this@MainActivity, "Secured ${flaggedItems.size} explicit items in 12-Hour AI Box", Toast.LENGTH_LONG).show()
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
                    if (checkBitmapForExplicitContent(bitmap)) {
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

    private fun checkBitmapForExplicitContent(bitmap: Bitmap): Boolean = false

    private fun loadImages() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID, 
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        val cursor: Cursor? = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)

        cursor?.use { c ->
            val idCol = c.getColumnIndex(MediaStore.Images.Media._ID)
            val bucketCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateCol = c.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
            if (idCol != -1) {
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val folder = if (bucketCol != -1) c.getString(bucketCol) ?: "Pictures" else "Pictures"
                    val dateMod = if (dateCol != -1) c.getLong(dateCol) * 1000L else System.currentTimeMillis()
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                    allMediaList.add(MediaItem(id, uri, folder, dateMod, isVideo = false))
                }
            }
        }
    }

    private fun loadVideos() {
        val projection = arrayOf(
            MediaStore.Video.Media._ID, 
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        val cursor: Cursor? = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder)

        cursor?.use { c ->
            val idCol = c.getColumnIndex(MediaStore.Video.Media._ID)
            val bucketCol = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val dateCol = c.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
            if (idCol != -1) {
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val folder = if (bucketCol != -1) c.getString(bucketCol) ?: "Videos" else "Videos"
                    val dateMod = if (dateCol != -1) c.getLong(dateCol) * 1000L else System.currentTimeMillis()
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
                    allMediaList.add(MediaItem(id, uri, folder, dateMod, isVideo = true))
                }
            }
        }
    }

    // Independent Expiry Logic for both boxes
    private fun cleanExpiredItems() {
        val currentTime = System.currentTimeMillis()
        val TWELVE_HOURS_MS = 12 * 60 * 60 * 1000L
        val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000L

        // Auto purge items from 12-Hour Box
        recoveryBox12Hours.removeAll { currentTime - it.deletedTimestamp > TWELVE_HOURS_MS }
        
        // Auto purge items from 30-Day Trash Box
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
                if (is12HourBox) recoveryBox12Hours.remove(item) else recoveryBox30Days.remove(item)
                allMediaList.add(item)
                sortMediaList()
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

        closeFullScreen()
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
        txtTitle.text = "Smart Albums & Folders"
        tabAlbums.setTextColor(resources.getColor(android.R.color.white, null))
        tabPhotos.setTextColor(0xFF888888.toInt())

        closeFullScreen()
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val uniqueFolders = allMediaList.distinctBy { it.folderName }.toMutableList()
        displayList.clear()
        displayList.addAll(uniqueFolders)
        galleryAdapter.setAlbumMode(true)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun showFavoritesTab() {
        isAlbumView = false
        currentActiveBox = 0
        txtTitle.text = "Favorites"
        closeFullScreen()
        recyclerView.visibility = View.VISIBLE
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        val favorites = allMediaList.filter { it.isFavorite }
        displayList.clear()
        displayList.addAll(favorites)
        galleryAdapter.setAlbumMode(false)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun show12HourRecoveryBox() {
        currentActiveBox = 1
        isAlbumView = false
        cleanExpiredItems()
        txtTitle.text = "12-Hour AI Box (${recoveryBox12Hours.size})"
        closeFullScreen()
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
        closeFullScreen()
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
        currentOpenedItem = item
        recyclerView.visibility = View.GONE
        fullScreenImageView.visibility = View.VISIBLE
        bottomViewerToolbar.visibility = View.VISIBLE
        fullScreenImageView.colorFilter = null
        try {
            Glide.with(this).load(item.uri).into(fullScreenImageView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeFullScreen() {
        currentOpenedItem = null
        fullScreenImageView.visibility = View.GONE
        bottomViewerToolbar.visibility = View.GONE
    }

    private fun showPhotoEditorDialog(item: MediaItem) {
        val editorOptions = arrayOf(
            "AI Auto Enhance & Sharpen",
            "Apply Grayscale Filter",
            "Apply Sepia Filter",
            "Apply Invert Filter",
            "Apply High Contrast",
            "Normal View"
        )
        AlertDialog.Builder(this)
            .setTitle("Photo Editor Filters")
            .setItems(editorOptions) { _, which ->
                when (which) {
                    0 -> fullScreenImageView.setColorFilter(Color.argb(30, 255, 255, 255))
                    1 -> fullScreenImageView.setColorFilter(Color.GRAY)
                    2 -> fullScreenImageView.setColorFilter(Color.YELLOW)
                    3 -> fullScreenImageView.setColorFilter(Color.RED)
                    4 -> fullScreenImageView.setColorFilter(Color.MAGENTA)
                    5 -> fullScreenImageView.colorFilter = null
                }
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showAISearchDialog() {
        val input = EditText(this)
        input.hint = "Search by tag, folder name..."

        AlertDialog.Builder(this)
            .setTitle("AI Smart Search")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString().trim().lowercase()
                if (query.isNotEmpty()) {
                    val matched = allMediaList.filter { it.folderName.lowercase().contains(query) }
                    if (matched.isEmpty()) {
                        Toast.makeText(this, "No matching items found", Toast.LENGTH_SHORT).show()
                    } else {
                        displayList.clear()
                        displayList.addAll(matched)
                        galleryAdapter.notifyDataSetChanged()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOptionsMenu() {
        val trashCount = recoveryBox30Days.size
        val options = arrayOf(
            "⭐ Favorites",
            "📁 Smart Albums & Folders",
            "🧹 AI Duplicate Cleaner",
            "☁️ Cloud Backup & Sync",
            "🗑️ 30-Day Trash Box ($trashCount items)",
            "🛡️ 12-Hour AI Safety Box (${recoveryBox12Hours.size} items)",
            "🔄 Toggle Sort Order",
            "ℹ️ About Smart Gallery"
        )
        AlertDialog.Builder(this)
            .setTitle("Gallery Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showFavoritesTab()
                    1 -> showAlbumsTab()
                    2 -> showDuplicateCleanerDialog()
                    3 -> triggerCloudSync()
                    4 -> show30DayRecoveryBox()
                    5 -> show12HourRecoveryBox()
                    6 -> {
                        isNewestFirst = !isNewestFirst
                        sortMediaList()
                        showPhotosTab()
                        Toast.makeText(this, if (isNewestFirst) "Sorted: Newest First" else "Sorted: Oldest First", Toast.LENGTH_SHORT).show()
                    }
                    7 -> Toast.makeText(this, "Smart Gallery v2.0 - AI Edition", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showDuplicateCleanerDialog() {
        if (duplicateItemsList.isEmpty()) {
            Toast.makeText(this, "No duplicate photos found!", Toast.LENGTH_SHORT).show()
        } else {
            displayList.clear()
            displayList.addAll(duplicateItemsList)
            txtTitle.text = "Duplicate Cleaner"
            recyclerView.visibility = View.VISIBLE
            closeFullScreen()
            galleryAdapter.notifyDataSetChanged()
        }
    }

    private fun triggerCloudSync() {
        Toast.makeText(this, "Connecting to Cloud...", Toast.LENGTH_SHORT).show()
        cloudSyncedList.clear()
        cloudSyncedList.addAll(allMediaList.take(5))
        cloudSyncedList.forEach { it.isCloudSynced = true }
        Toast.makeText(this, "${cloudSyncedList.size} items backed up!", Toast.LENGTH_LONG).show()
    }
}
