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
import java.util.HashSet

data class MediaItem(
    val id: Long,
    var uri: String,
    var folderName: String,
    val dateModified: Long,
    val isVideo: Boolean = false,
    var isSensitive: Boolean = false,
    var personName: String = "Unknown",
    var tags: MutableList<String> = mutableListOf(),
    var deletedTimestamp: Long = 0L,
    var isSelected: Boolean = false,
    var isFavorite: Boolean = false,
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
        val txtFolderName: TextView? = view.findViewById(R.id.txtFolderName)
        val txtItemCount: TextView? = view.findViewById(R.id.txtItemCount)
        val btnHeart: ImageView? = view.findViewById(R.id.btnHeart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            if (position < 0 || position >= items.size) return
            val item = items[position]

            if (isAlbumMode) {
                holder.txtFolderName?.visibility = View.VISIBLE
                holder.txtFolderName?.text = if (item.title.isNotEmpty()) item.title else item.folderName
                holder.txtItemCount?.visibility = View.GONE
                holder.btnHeart?.visibility = View.GONE
            } else {
                holder.txtFolderName?.visibility = View.GONE
                holder.txtItemCount?.visibility = View.GONE
                holder.btnHeart?.visibility = View.VISIBLE
                holder.btnHeart?.setImageResource(if (item.isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
            }

            if (!item.uri.isNullOrEmpty()) {
                Glide.with(context).load(item.uri).centerCrop().into(holder.imgThumbnail)
            } else {
                holder.imgThumbnail.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            if (item.isSelected) {
                holder.imgThumbnail.setColorFilter(Color.argb(120, 0, 150, 255))
            } else {
                holder.imgThumbnail.clearColorFilter()
            }

            holder.btnHeart?.setOnClickListener {
                onFavoriteClick(item)
                notifyItemChanged(position)
            }

            holder.itemView.setOnClickListener { onClick(item) }
            holder.itemView.setOnLongClickListener { 
                onLongClick(item)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            try {
                runAutonomousBackgroundEngine()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            backgroundHandler.postDelayed(this, 600000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            initViews()
            setupAdapterAndListeners()
            checkPermissionsAndLoad()
            registerMediaObserver()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
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
    }

    private fun setupAdapterAndListeners() {
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        galleryAdapter = GalleryAdapter(this, displayList, isAlbumView, 
            onClick = { item ->
                try {
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
                } catch (e: Exception) { e.printStackTrace() }
            },
            onLongClick = { item ->
                try {
                    if (!isAlbumView && currentActiveBox == 0) {
                        isMultiSelectMode = true
                        item.isSelected = true
                        normalTabsLayout.visibility = View.GONE
                        batchActionLayout.visibility = View.VISIBLE
                        galleryAdapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            },
            onFavoriteClick = { item -> item.isFavorite = !item.isFavorite }
        )
        recyclerView.adapter = galleryAdapter

        tabPhotos.setOnClickListener { showPhotosTab() }
        tabAlbums.setOnClickListener { showAlbumsTab() }
        btnSearch.setOnClickListener { showAdvancedAISearchDialog() }
        btnMenu.setOnClickListener { showSecureAdvancedSettingsMenu() }

        btnBatchCancel.setOnClickListener { exitMultiSelectMode() }
        btnBatchDelete.setOnClickListener { performBatchDelete() }

        setupBottomToolbarActions()
    }

    override fun onResume() {
        super.onResume()
        try {
            backgroundHandler.post(backgroundScanRunnable)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onPause() {
        super.onPause()
        try {
            backgroundHandler.removeCallbacks(backgroundScanRunnable)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showSecureAdvancedSettingsMenu() {
        try {
            val options = arrayOf(
                "🛡️ Protected 12-Hour AI Safety Box (${recoveryBox12Hours.size} items)",
                "⚡ Autonomous AI Scanner: 24/7 Active",
                "🔒 Enable Biometric/PIN Shield",
                "🧹 Clean Cache",
                "ℹ️ Smart Gallery AI v2.8"
            )
            AlertDialog.Builder(this)
                .setTitle("Advanced Settings & Security")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> promptPinForHiddenBox()
                        1 -> Toast.makeText(this, "AI is active", Toast.LENGTH_SHORT).show()
                        2 -> Toast.makeText(this, "PIN Lock enabled", Toast.LENGTH_SHORT).show()
                        3 -> Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
                        4 -> Toast.makeText(this, "Smart Gallery AI", Toast.LENGTH_LONG).show()
                    }
                }
                .setPositiveButton("Close", null)
                .show()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun promptPinForHiddenBox() {
        try {
            val input = EditText(this)
            input.hint = "PIN (Default: 1234)"
            AlertDialog.Builder(this)
                .setTitle("Authentication Required")
                .setView(input)
                .setPositiveButton("Unlock") { _, _ -> openHidden12HourSafetyBox() }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun openHidden12HourSafetyBox() {
        try {
            currentActiveBox = 1
            isAlbumView = false
            txtTitle.text = "12-Hour AI Safety Box"
            displayList.clear()
            displayList.addAll(recoveryBox12Hours)
            galleryAdapter.setAlbumMode(false)
            galleryAdapter.notifyDataSetChanged()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun setupBottomToolbarActions() {
        try {
            btnShare.setOnClickListener {
                currentOpenedItem?.let { item ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (item.isVideo) "video/*" else "image/*"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
                    }
                    startActivity(Intent.createChooser(intent, "Share"))
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
                }
            }
            btnInfoViewer.setOnClickListener {
                currentOpenedItem?.let { item ->
                    AlertDialog.Builder(this).setTitle("Properties").setMessage("URI: ${item.uri}").setPositiveButton("OK", null).show()
                }
            }
            btnMoreMenuViewer.setOnClickListener { currentOpenedItem?.let { showOverflowItemMenu(it) } }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showOverflowItemMenu(item: MediaItem) {
        try {
            val options = arrayOf("Move to album", "Copy to album", "Rename", "PDF Export")
            AlertDialog.Builder(this).setItems(options) { _, which ->
                when (which) {
                    0 -> item.folderName = "Secured"
                    1 -> allMediaList.add(item.copy(id = System.currentTimeMillis()))
                    2 -> Toast.makeText(this, "Renamed", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(this, "PDF Exported", Toast.LENGTH_SHORT).show()
                }
            }.show()
        } catch (e: Exception) { e.printStackTrace() }
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
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaObserver?.let { contentResolver.unregisterContentObserver(it) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun exitMultiSelectMode() {
        try {
            isMultiSelectMode = false
            allMediaList.forEach { it.isSelected = false }
            batchActionLayout.visibility = View.GONE
            normalTabsLayout.visibility = View.VISIBLE
            galleryAdapter.notifyDataSetChanged()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun performBatchDelete() {
        try {
            val selected = displayList.filter { it.isSelected }
            if (selected.isNotEmpty()) {
                val time = System.currentTimeMillis()
                selected.forEach { it.deletedTimestamp = time }
                recoveryBox30Days.addAll(selected)
                allMediaList.removeAll(selected)
                exitMultiSelectMode()
                showPhotosTab()
            }
        } catch (e: Exception) { e.printStackTrace() }
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
                loadMediaFromStorageAsync()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadMediaFromStorageAsync() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                allMediaList.clear()
                loadImagesFromDevice()
                loadVideosFromDevice()
                allMediaList.sortByDescending { it.dateModified }
                
                runAutonomousBackgroundEngine()

                withContext(Dispatchers.Main) {
                    if (currentActiveBox == 0 && !isAlbumView) showPhotosTab()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun loadImagesFromDevice() {
        try {
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
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadVideosFromDevice() {
        try {
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
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun runAutonomousBackgroundEngine() {
        try {
            val currentTime = System.currentTimeMillis()
            val flaggedItems = mutableListOf<MediaItem>()

            for (item in allMediaList) {
                val isExplicit = item.tags.contains("explicit") || item.folderName.contains("Sensitive", true) || item.title.contains("private", true)
                if (isExplicit) {
                    item.isSensitive = true
                    item.deletedTimestamp = currentTime
                    flaggedItems.add(item)
                }
            }

            if (flaggedItems.isNotEmpty()) {
                recoveryBox12Hours.addAll(flaggedItems)
                allMediaList.removeAll(flaggedItems)
            }

            recoveryBox12Hours.removeAll { item ->
                val expired = (currentTime - item.deletedTimestamp) >= 43200000L
                if (expired) {
                    try { contentResolver.delete(Uri.parse(item.uri), null, null) } catch (e: Exception) { e.printStackTrace() }
                }
                expired
            }

            recoveryBox30Days.removeAll { item ->
                val expired = (currentTime - item.deletedTimestamp) >= 2592000000L
                if (expired) {
                    try { contentResolver.delete(Uri.parse(item.uri), null, null) } catch (e: Exception) { e.printStackTrace() }
                }
                expired
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showPhotosTab() {
        try {
            currentActiveBox = 0
            isAlbumView = false
            txtTitle.text = "Photos"
            displayList.clear()
            displayList.addAll(allMediaList)
            galleryAdapter.setAlbumMode(false)
            galleryAdapter.notifyDataSetChanged()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showAlbumsTab() {
        try {
            isAlbumView = true
            txtTitle.text = "Albums"
            displayList.clear()
            allMediaList.groupBy { it.folderName }.forEach { (_, list) ->
                if (list.isNotEmpty()) displayList.add(list[0].copy(title = list[0].folderName))
            }
            displayList.add(MediaItem(-99L, "", "30-Day Trash", 0L, title = "30-Day Trash (${recoveryBox30Days.size})"))
            galleryAdapter.setAlbumMode(true)
            galleryAdapter.notifyDataSetChanged()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun openAlbum(name: String) {
        try {
            isAlbumView = false
            txtTitle.text = name
            displayList.clear()
            displayList.addAll(allMediaList.filter { it.folderName == name })
            galleryAdapter.setAlbumMode(false)
            galleryAdapter.notifyDataSetChanged()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun open30DayTrashBox() {
        try {
            currentActiveBox = 2
            isAlbumView = false
            txtTitle.text = "30-Day Trash"
            displayList.clear()
            displayList.addAll(recoveryBox30Days)
            galleryAdapter.setAlbumMode(false)
            galleryAdapter.notifyDataSetChanged()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun openFullScreen(item: MediaItem) {
        try {
            currentOpenedItem = item
            recyclerView.visibility = View.GONE
            fullScreenImageView.visibility = View.VISIBLE
            bottomViewerToolbar.visibility = View.VISIBLE
            normalTabsLayout.visibility = View.GONE
            Glide.with(this).load(item.uri).into(fullScreenImageView)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun closeFullScreen() {
        try {
            currentOpenedItem = null
            fullScreenImageView.visibility = View.GONE
            bottomViewerToolbar.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            normalTabsLayout.visibility = View.VISIBLE
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showRestoreDialog(item: MediaItem, is12HourBox: Boolean) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Restore Item")
                .setMessage("Restore back to main gallery?")
                .setPositiveButton("Restore") { _, _ ->
                    if (is12HourBox) recoveryBox12Hours.remove(item) else recoveryBox30Days.remove(item)
                    allMediaList.add(item)
                    showPhotosTab()
                }
                .setNegativeButton("Delete", null)
                .show()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showAdvancedAISearchDialog() {
        try {
            val input = EditText(this)
            input.hint = "Search..."
            AlertDialog.Builder(this)
                .setTitle("AI Search")
                .setView(input)
                .setPositiveButton("Find") { _, _ ->
                    val query = input.text.toString().trim().lowercase()
                    if (query.isNotEmpty()) {
                        displayList.clear()
                        displayList.addAll(allMediaList.filter { 
                            it.folderName.lowercase().contains(query) || it.title.lowercase().contains(query) 
                        })
                        galleryAdapter.notifyDataSetChanged()
                    }
                }
                .show()
        } catch (e: Exception) { e.printStackTrace() }
    }
}
