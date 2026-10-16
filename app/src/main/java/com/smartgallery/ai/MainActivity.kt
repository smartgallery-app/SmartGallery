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
import com.smartgallery.app.R

data class MediaItem(
    val id: Long,
    val uri: String,
    val folderName: String,
    val isVideo: Boolean = false,
    var isSensitive: Boolean = false,
    var tags: List<String> = emptyList()
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

    private lateinit var recyclerView: RecyclerView
    private lateinit var fullScreenImageView: ImageView
    private lateinit var txtTitle: TextView
    private lateinit var tabPhotos: TextView
    private lateinit var tabAlbums: TextView
    private lateinit var btnSearch: ImageView
    private lateinit var btnMenu: ImageView

    private lateinit var galleryAdapter: GalleryAdapter
    private var isAlbumView = false

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
            if (isAlbumView) {
                openAlbum(item.folderName)
            } else {
                openFullScreen(item.uri)
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

        if (allMediaList.isEmpty()) {
            Toast.makeText(this, "No media found on device!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Loaded ${allMediaList.size} media items", Toast.LENGTH_SHORT).show()
        }

        showPhotosTab()
    }

    private fun loadImages() {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

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
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

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

    private fun showPhotosTab() {
        isAlbumView = false
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
        val matched = allMediaList.filter { item ->
            item.tags.any { tag -> tag.contains(query) }
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
        val options = arrayOf("Scan & Auto Delete Explicit Content (AI)", "Settings", "About")
        AlertDialog.Builder(this)
            .setTitle("Menu Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> runAISafetyScanAndDelete()
                    1 -> Toast.makeText(this, "Settings Selected", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "Smart Gallery v1.0", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun runAISafetyScanAndDelete() {
        Toast.makeText(this, "AI Deep Scanning (Checking 1-min mark in Videos)...", Toast.LENGTH_SHORT).show()

        val itemsToDelete = mutableListOf<MediaItem>()

        allMediaList.forEach { item ->
            val uri = Uri.parse(item.uri)

            if (item.isVideo) {
                // Video Logic: Extract frame at 60 seconds (1 minute mark)
                val isSensitiveVideo = scanVideoAtOneMinute(uri)
                if (isSensitiveVideo) {
                    itemsToDelete.add(item)
                }
            } else {
                // Image Logic
                AIScannerHelper.scanImageForSafetyAndTags(this, uri) { isSensitive, tags ->
                    item.tags = tags
                    if (isSensitive) {
                        itemsToDelete.add(item)
                    }
                }
            }
        }

        if (itemsToDelete.isNotEmpty()) {
            itemsToDelete.forEach { item ->
                try {
                    contentResolver.delete(Uri.parse(item.uri), null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            allMediaList.removeAll(itemsToDelete)
            Toast.makeText(this, "AI Cleaned ${itemsToDelete.size} explicit items!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Gallery is Safe. No explicit media found.", Toast.LENGTH_SHORT).show()
        }

        showPhotosTab()
    }

    // Video ke 60th second par frame nikal kar check karne ka logic
    private fun scanVideoAtOneMinute(videoUri: Uri): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, videoUri)
            // 60 seconds = 60,000,000 microseconds
            val frameBitmap = retriever.getFrameAtTime(60_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            
            var isExplicit = false
            if (frameBitmap != null) {
                // Bitmap ko AI Scanner se scan karwa rahe hain
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
        } else if (txtTitle.text != "Photos" && txtTitle.text != "Albums") {
            showAlbumsTab()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            loadMediaFromStorage()
        } else {
            Toast.makeText(this, "Storage Permission Denied!", Toast.LENGTH_SHORT).show()
        }
    }
}
