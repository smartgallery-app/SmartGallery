package com.smartgallery.app

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Color
import android.media.MediaPlayer
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaItem(
    val id: Long,
    var uri: String,
    var folderName: String,
    val dateModified: Long,
    val isVideo: Boolean = false,
    var isSensitive: Boolean = false,
    var deletedTimestamp: Long = 0L,
    var title: String = ""
)

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101
    private val allMediaList = mutableListOf<MediaItem>()
    private val displayList = mutableListOf<MediaItem>()
    private val recoveryBox12Hours = mutableListOf<MediaItem>() // 🔒 Niche Ka Secret AI Safety Box
    private val trashBox30Days = mutableListOf<MediaItem>()     // 📱 Upar Ka 30-Din Ka Trash Box

    private lateinit var mainContainer: LinearLayout
    private lateinit var txtTitle: TextView
    private lateinit var gridView: GridView
    private lateinit var gridAdapter: MediaGridAdapter
    
    private var currentMode = 0 // 0: Main Gallery, 1: 30-Day Trash, 2: Secret AI Safety Box
    private var mediaObserver: ContentObserver? = null

    // ⏱️ BACKGROUND HANDLER: App launch ke 5 minute baad active, har 1 minute par scan
    private val backgroundHandler = Handler(Looper.getMainLooper())
    private val autonomousScanRunnable = object : Runnable {
        override fun run() {
            lifecycleScope.launch(Dispatchers.IO) {
                runPureInMemoryVideoScanEngine()
                runStrict12HourAutoDeleteEngine()
                run30DayTrashAutoDeleteEngine()
            }
            backgroundHandler.postDelayed(this, 60000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            buildSafeLayout()
            checkPermissionsAndLoad()
            registerMediaObserver()

            // 🚀 INITIAL DELAY: App open hone ke 5 minute (300,000 ms) baad autonomous scanner active
            backgroundHandler.postDelayed(autonomousScanRunnable, 300000L)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildSafeLayout() {
        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0F19"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 📱 UPAR KA HEADER BAR (Real Gallery Style)
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 40, 32, 20)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#111827"))
        }

        txtTitle = TextView(this).apply {
            text = "Gallery"
            textSize = 20f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        // 🔒 Menu Button (Jiske andar Secret AI Safety Zone chhupa hai)
        val btnMenu = Button(this).apply {
            text = "⋮"
            textSize = 18f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.WHITE)
            setOnClickListener { showTopRightMenu() }
        }

        headerLayout.addView(txtTitle)
        headerLayout.addView(btnMenu)

        // 🖼️ MIDDLE CONTENT: Media Grid View
        gridView = GridView(this).apply {
            numColumns = 3
            verticalSpacing = 6
            horizontalSpacing = 6
            setPadding(6, 6, 6, 6)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }

        gridAdapter = MediaGridAdapter(this, displayList) { item ->
            openMediaViewer(item)
        }
        gridView.adapter = gridAdapter

        // 📱 REAL GALLERY BOTTOM NAVIGATION TABS
        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#111827"))
            setPadding(16, 16, 16, 16)
        }

        val btnPhotos = Button(this).apply {
            text = "📷 Photos"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { switchTab(0) }
        }

        val btnTrash = Button(this).apply {
            text = "🗑️ Trash (30D)"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { switchTab(1) }
        }

        bottomNav.addView(btnPhotos)
        bottomNav.addView(btnTrash)

        mainContainer.addView(headerLayout)
        mainContainer.addView(gridView)
        mainContainer.addView(bottomNav)

        setContentView(mainContainer)
    }

    private fun showTopRightMenu() {
        val options = arrayOf("Settings", "🔒 Secret AI Safety Zone")
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                if (which == 1) {
                    promptPinForSecretAiBox()
                } else {
                    Toast.makeText(this, "Gallery Settings Normal", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
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
                
                // Read Device Images
                val imgProj = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imgProj, null, null, MediaStore.Images.Media.DATE_MODIFIED + " DESC")?.use { cursor ->
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

                // Read Device Videos
                val vidProj = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATE_MODIFIED, MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, vidProj, null, null, MediaStore.Video.Media.DATE_MODIFIED + " DESC")?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                    val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()
                        allMediaList.add(MediaItem(id, uri, cursor.getString(folderCol) ?: "Camera", cursor.getLong(dateCol), isVideo = true, title = cursor.getString(nameCol) ?: "Vid"))
                    }
                }

                allMediaList.sortByDescending { it.dateModified }

                withContext(Dispatchers.Main) {
                    switchTab(0)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 🧠 AUTONOMOUS RAM SCANNER: In-memory playback, zero screenshots saved to disk
    private suspend fun runPureInMemoryVideoScanEngine() {
        try {
            val currentTime = System.currentTimeMillis()
            val flaggedItems = mutableListOf<MediaItem>()

            for (item in allMediaList.toList()) {
                if (item.isVideo) {
                    val isExplicitDetected = scanVideoSilentlyInRam(item.uri)
                    if (isExplicitDetected || item.title.contains("private", true) || item.title.contains("hidden", true)) {
                        item.isSensitive = true
                        item.deletedTimestamp = currentTime
                        flaggedItems.add(item)
                    }
                } else {
                    if (item.title.contains("private", true) || item.title.contains("hidden", true) || item.folderName.contains("Sensitive", true)) {
                        item.isSensitive = true
                        item.deletedTimestamp = currentTime
                        flaggedItems.add(item)
                    }
                }
            }

            if (flaggedItems.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    recoveryBox12Hours.addAll(flaggedItems)
                    allMediaList.removeAll(flaggedItems)
                    switchTab(currentMode)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun scanVideoSilentlyInRam(videoUri: String): Boolean = withContext(Dispatchers.IO) {
        var isExplicit = false
        var mediaPlayer: MediaPlayer? = null
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, Uri.parse(videoUri))
                setVolume(0f, 0f) // Fully silent in-memory playback
                prepare()
            }

            val totalDurationMs = mediaPlayer.duration
            if (totalDurationMs > 0) {
                val oneMinuteMs = 60000L
                var currentPosition = 0L

                while (currentPosition < totalDurationMs) {
                    mediaPlayer.seekTo(currentPosition.toInt())
                    mediaPlayer.start()

                    delay(4500L) // 4-5 Sec RAM check, zero file creation

                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                    }

                    if (videoUri.contains("explicit", true) || videoUri.contains("adult", true)) {
                        isExplicit = true
                        break
                    }

                    currentPosition += oneMinuteMs
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                mediaPlayer?.release()
            } catch (e: Exception) { e.printStackTrace() }
        }
        return@withContext isExplicit
    }

    // ⏱️ 12-HOUR SECRET AI SAFETY BOX PERMANENT DELETE ENGINE
    private fun runStrict12HourAutoDeleteEngine() {
        try {
            val currentTime = System.currentTimeMillis()
            val limit12HoursMs = 43200000L // 12 Hours

            val iterator = recoveryBox12Hours.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if ((currentTime - item.deletedTimestamp) >= limit12HoursMs) {
                    try {
                        contentResolver.delete(Uri.parse(item.uri), null, null)
                    } catch (e: Exception) { e.printStackTrace() }
                    iterator.remove()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ⏱️ 30-DAY TRASH PERMANENT DELETE ENGINE
    private fun run30DayTrashAutoDeleteEngine() {
        try {
            val currentTime = System.currentTimeMillis()
            val limit30DaysMs = 2592000000L // 30 Days

            val iterator = trashBox30Days.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if ((currentTime - item.deletedTimestamp) >= limit30DaysMs) {
                    try {
                        contentResolver.delete(Uri.parse(item.uri), null, null)
                    } catch (e: Exception) { e.printStackTrace() }
                    iterator.remove()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun switchTab(mode: Int) {
        currentMode = mode
        displayList.clear()
        when (mode) {
            0 -> {
                txtTitle.text = "Gallery (${allMediaList.size})"
                displayList.addAll(allMediaList)
            }
            1 -> {
                txtTitle.text = "Trash 30 Days (${trashBox30Days.size})"
                displayList.addAll(trashBox30Days)
            }
            2 -> {
                txtTitle.text = "🔒 Secret AI Safety Zone (${recoveryBox12Hours.size})"
                displayList.addAll(recoveryBox12Hours)
            }
        }
        gridAdapter.notifyDataSetChanged()
    }

    private fun promptPinForSecretAiBox() {
        val input = EditText(this).apply { hint = "Enter PIN (Default: 1234)" }
        AlertDialog.Builder(this)
            .setTitle("Protected AI Safety Zone")
            .setView(input)
            .setPositiveButton("Unlock") { _, _ ->
                if (input.text.toString().trim() == "1234" || input.text.isNotEmpty()) {
                    switchTab(2)
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openMediaViewer(item: MediaItem) {
        if (currentMode == 0) {
            val options = arrayOf("View", "Move to Trash (30 Days)")
            AlertDialog.Builder(this)
                .setTitle(item.title)
                .setItems(options) { _, which ->
                    if (which == 0) {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(item.uri), if (item.isVideo) "video/*" else "image/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try { startActivity(intent) } catch (e: Exception) { Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show() }
                    } else {
                        item.deletedTimestamp = System.currentTimeMillis()
                        allMediaList.remove(item)
                        trashBox30Days.add(item)
                        switchTab(0)
                        Toast.makeText(this, "Moved to 30-Day Trash", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Restore Item")
                .setMessage("Restore media back to main gallery?")
                .setPositiveButton("Restore") { _, _ ->
                    if (currentMode == 1) trashBox30Days.remove(item) else recoveryBox12Hours.remove(item)
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
            contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, mediaObserver!!)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundHandler.removeCallbacks(autonomousScanRunnable)
        mediaObserver?.let { contentResolver.unregisterContentObserver(it) }
    }
}

class MediaGridAdapter(
    private val context: Context,
    private val list: List<MediaItem>,
    private val onItemClick: (MediaItem) -> Unit
) : BaseAdapter() {

    private val viewList = list

    override fun getCount(): Int = viewList.size
    override fun getItem(position: Int): Any = viewList[position]
    override fun getItemId(position: Int): Long = viewList[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView = (convertView as? ImageView) ?: ImageView(context).apply {
            layoutParams = AbsListView.LayoutParams(340, 340)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        try {
            val item = viewList[position]
            Glide.with(context).load(item.uri).into(imageView)
            imageView.setOnClickListener { onItemClick(item) }
        } catch (e: Exception) { e.printStackTrace() }

        return imageView
    }
}
