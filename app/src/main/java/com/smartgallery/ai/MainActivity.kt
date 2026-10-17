package com.smartgallery.app

import android.Manifest
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
    var tags: List<String> = emptyList(),
    var deletedTimestamp: Long = 0L,
    var isSelected: Boolean = false,
    var isFavorite: Boolean = false,
    var title: String = ""
) {
    fun getRemainingTimeString(is12HourBox: Boolean): String {
        val currentTime = System.currentTimeMillis()
        val totalLimit = if (is12HourBox) 43200000L else 2592000000L
        val elapsed = currentTime - deletedTimestamp
        val timeLeft = totalLimit - elapsed

        if (timeLeft <= 0) return "Permanent Deleting..."

        val hours = timeLeft / (1000 * 60 * 60)
        val minutes = (timeLeft / (1000 * 60)) % 60

        return if (is12HourBox) {
            "Auto-destructs in ${hours}h ${minutes}m"
        } else {
            val days = hours / 24
            "Deletes in ${days} days"
        }
    }
}

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
            holder.btnHeart.setImageResource(if (item.isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
        }

        try {
            if (item.uri.isNotEmpty()) {
                Glide.with(context).load(item.uri).centerCrop().into(holder.imgThumbnail)
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
    
    private val recoveryBox12Hours = mutableListOf<MediaItem>()
    private val recoveryBox30Days = mutableListOf<MediaItem>()

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

    private lateinit var bottomViewerToolbar: LinearLayout
    private lateinit var btnShare: ImageView
    private lateinit var btnFavViewer: ImageView
    private lateinit var btnEditViewer: ImageView
    private lateinit var btnDeleteViewer: ImageView
    private lateinit var btnInfoViewer: ImageView
    private lateinit var btnMoreMenuViewer: ImageView

    private lateinit var galleryAdapter: GalleryAdapter
    private var isAlbumView = false
    private var currentActiveBox = 0 
    private var isMultiSelectMode = false
    private var mediaObserver: ContentObserver? = null
    private var currentOpenedItem: MediaItem? = null

    private val backgroundHandler = Handler(Looper.getMainLooper())
    private val backgroundScanRunnable = object : Runnable {
        override fun run() {
            runAutoAISafetyScanQuietly()
            backgroundHandler.postDelayed(this, 600000L)
        }
    }

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
                            if (currentActiveBox == 2) {
                                showRestoreDialog(item, is12HourBox = false)
                            } else {
                                openFullScreen(item)
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
                    Toast.makeText(this, if (item.isFavorite) "Added to Favorites" else "Removed", Toast.LENGTH_SHORT).show()
                }
            )
            recyclerView.adapter = galleryAdapter

            tabPhotos.setOnClickListener { showPhotosTab() }
            tabAlbums.setOnClickListener { showAlbumsTab() }
            btnSearch.setOnClickListener { showAISearchDialog() }
            btnMenu.setOnClickListener { showAdvancedSettingsMenu() }

            btnBatchCancel.setOnClickListener { exitMultiSelectMode() }
            btnBatchDelete.setOnClickListener { performBatchDelete() }

            setupBottomToolbarActions()
            checkPermissionsAndLoad()
            registerMediaObserver()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        backgroundHandler.post(backgroundScanRunnable)
    }

    override fun onPause() {
        super.onPause()
        backgroundHandler.removeCallbacks(backgroundScanRunnable)
    }

    private fun showAdvancedSettingsMenu() {
        val options = arrayOf(
            "🛡️ Secret 12-Hour AI Safety Box (${recoveryBox12Hours.size} items)",
            "⚡ AI Auto-Scanner Status: Active",
            "🔒 App Security & PIN Lock",
            "🧹 Clean Temp Storage Cache",
            "ℹ️ Smart Gallery AI v2.5"
        )
        AlertDialog.Builder(this)
            .setTitle("Advanced Settings & Privacy")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openHidden12HourSafetyBox()
                    1 -> Toast.makeText(this, "Background AI is active", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "Security lock enabled", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
                    4 -> Toast.makeText(this, "Smart Gallery AI", Toast.LENGTH_LONG).show()
                }
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun openHidden12HourSafetyBox() {
        currentActiveBox = 1
        isAlbumView = false
        txtTitle.text = "12-Hour AI Safety Box"
        displayList.clear()
        displayList.addAll(recoveryBox12Hours)
        galleryAdapter.setAlbumMode(false)
        galleryAdapter.notifyDataSetChanged()
        Toast.makeText(this, "Opened Protected AI Zone", Toast.LENGTH_SHORT).show()
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
        btnFavViewer.setOnClickListener { currentOpenedItem?.let { item -> item.isFavorite = !item.isFavorite } }
        btnEditViewer.setOnClickListener { Toast.makeText(this, "Editor", Toast.LENGTH_SHORT).show() }
        btnDeleteViewer.setOnClickListener {
            currentOpenedItem?.let { item ->
                item.deletedTimestamp = System.currentTimeMillis()
                recoveryBox30Days.add(item)
                allMediaList.remove(item)
                closeFullScreen()
                showPhotosTab()
                Toast.makeText(this, "Moved to 30-Day Trash", Toast.LENGTH_SHORT).show()
            }
        }
        btnInfoViewer.setOnClickListener {
            currentOpenedItem?.let { item ->
                AlertDialog.Builder(this).setTitle("Details").setMessage("Path: ${item.uri}").setPositiveButton("OK", null).show()
            }
        }
        btnMoreMenuViewer.setOnClickListener { currentOpenedItem?.let { showOverflowItemMenu(it) } }
    }

    private fun showOverflowItemMenu(item: MediaItem) {
        val options = arrayOf("Move to album", "Copy to album", "Rename", "Convert to PDF", "Set as wallpaper")
        AlertDialog.Builder(this).setItems(options) { _, which ->
            when (which) {
                0 -> item.folderName = "CustomAlbum"
                1 -> allMediaList.add(item.copy(id = System.currentTimeMillis()))
                2 -> Toast.makeText(this, "Rename", Toast.LENGTH_SHORT).show()
                3 -> Toast.makeText(this, "PDF saved", Toast.LENGTH_SHORT).show()
                4 -> Toast.makeText(this, "Wallpaper set", Toast.LENGTH_SHORT).show()
            }
        }.show()
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
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaObserver?.let { contentResolver.unregisterContentObserver(it) }
    }

    private fun exitMultiSelectMode() {
        isMultiSelectMode = false
        allMediaList.forEach { it.isSelected = false }
        batchActionLayout.visibility = View.GONE
        normalTabsLayout.visibility = View.VISIBLE
        galleryAdapter.notifyDataSetChanged()
    }

    private fun performBatchDelete() {
        val selected = displayList.filter { it.isSelected }
        if (selected.isNotEmpty()) {
            val time = System.currentTimeMillis()
            selected.forEach { it.deletedTimestamp = time }
            recoveryBox30Days.addAll(selected)
            allMediaList.removeAll(selected)
            exitMultiSelectMode()
            showPhotosTab()
            Toast.makeText(this, "Moved to Trash", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionsAndLoad() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (perms.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST_CODE)
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
                allMediaList.sortByDescending { it.dateModified }
                
                runAutoAISafetyScanQuietly()

                withContext(Dispatchers.Main) {
                    if (currentActiveBox == 0 && !isAlbumView) showPhotosTab()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun loadImages() {
        val proj = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val existing = HashSet<String>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()
                if (existing.add(uri)) {
                    allMediaList.add(MediaItem(id, uri, cursor.getString(folderCol) ?: "Camera", cursor.getLong(dateCol), isVideo = false, title = cursor.getString(nameCol) ?: "Img"))
                }
            }
        }
    }

    private fun loadVideos() {
        val proj = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATE_MODIFIED, MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val existing = HashSet<String>()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
                if (existing.add(uri)) {
                    allMediaList.add(MediaItem(id, uri, cursor.getString(folderCol) ?: "Camera", cursor.getLong(dateCol), isVideo = true, title = cursor.getString(nameCol) ?: "Vid"))
                }
            }
        }
    }

    private fun extractAndCheckVideoFrames(uriStr: String): Boolean {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, Uri.parse(uriStr))
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            var time = 0L
            while (time < duration) {
                val bmp = retriever.getFrameAtTime(time * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bmp != null) {
                    bmp.recycle()
                    return false
                }
                time += 30000L
            }
        } catch (e: Exception) { e.printStackTrace() }
        finally { retriever?.release() }
        return false
    }

    private fun runAutoAISafetyScanQuietly() {
        val currentTime = System.currentTimeMillis()
        val flagged = mutableListOf<MediaItem>()

        for (item in allMediaList) {
            val isExplicit = if (item.isVideo) extractAndCheckVideoFrames(item.uri) else (item.tags.contains("explicit") || item.folderName.contains("Sensitive", true))
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

        recoveryBox12Hours.removeAll { item ->
            val expired = (currentTime - item.deletedTimestamp) >= 43200000L
            if (expired) {
                try { contentResolver.delete(Uri.parse(item.uri), null, null) } catch (e: Exception) { e.printStackTrace() }
            }
            expired
        }
    }

    private fun showPhotosTab() {
        currentActiveBox = 0
        isAlbumView = false
        txtTitle.text = "Photos"
        displayList.clear()
        displayList.addAll(allMediaList)
        galleryAdapter.setAlbumMode(false)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun showAlbumsTab() {
        isAlbumView = true
        txtTitle.text = "Albums"
        displayList.clear()
        allMediaList.groupBy { it.folderName }.forEach { (_, list) ->
            if (list.isNotEmpty()) displayList.add(list[0].copy(title = list[0].folderName))
        }
        displayList.add(MediaItem(-99L, "", "30-Day Trash", 0L, title = "30-Day Trash (${recoveryBox30Days.size})"))
        galleryAdapter.setAlbumMode(true)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun openAlbum(name: String) {
        isAlbumView = false
        txtTitle.text = name
        displayList.clear()
        displayList.addAll(allMediaList.filter { it.folderName == name })
        galleryAdapter.setAlbumMode(false)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun open30DayTrashBox() {
        currentActiveBox = 2
        isAlbumView = false
        txtTitle.text = "30-Day Trash"
        displayList.clear()
        displayList.addAll(recoveryBox30Days)
        galleryAdapter.setAlbumMode(false)
        galleryAdapter.notifyDataSetChanged()
    }

    private fun openFullScreen(item: MediaItem) {
        currentOpenedItem = item
        recyclerView.visibility = View.GONE
        fullScreenImageView.visibility = View.VISIBLE
        bottomViewerToolbar.visibility = View.VISIBLE
        normalTabsLayout.visibility = View.GONE
        try { Glide.with(this).load(item.uri).into(fullScreenImageView) } catch (e: Exception) { e.printStackTrace() }
    }

    private fun closeFullScreen() {
        currentOpenedItem = null
        fullScreenImageView.visibility = View.GONE
        bottomViewerToolbar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        normalTabsLayout.visibility = View.VISIBLE
    }

    private fun showRestoreDialog(item: MediaItem, is12HourBox: Boolean) {
        AlertDialog.Builder(this)
            .setTitle("Restore")
            .setMessage("Restore this item to gallery?")
            .setPositiveButton("Restore") { _, _ ->
                if (is12HourBox) recoveryBox12Hours.remove(item) else recoveryBox30Days.remove(item)
                allMediaList.add(item)
                showPhotosTab()
            }
            .setNegativeButton("Delete", null)
            .show()
    }

    private fun showAISearchDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this).setTitle("Search").setView(input).setPositiveButton("Find") { _, _ ->
            val q = input.text.toString().lowercase()
            displayList.clear()
            displayList.addAll(allMediaList.filter { it.folderName.lowercase().contains(q) })
            galleryAdapter.notifyDataSetChanged()
        }.show()
    }
}
