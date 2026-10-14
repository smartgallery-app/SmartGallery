package com.smartgallery.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnPhotos: TextView
    private lateinit var btnAlbums: TextView
    private val PERMISSION_REQUEST_CODE = 100

    private val allPhotos = ArrayList<String>()
    private val albumMap = HashMap<String, ArrayList<String>>()
    private val albumNames = ArrayList<String>()
    private var isAlbumMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Root layout dynamically set kar rahe hain taaki XML ID crash na ho
            val rootLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.BLACK)
            }

            recyclerView = RecyclerView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                layoutManager = GridLayoutManager(this@MainActivity, 4)
            }
            rootLayout.addView(recyclerView)

            // Bottom Navigation Layout (Photos & Albums Tabs)
            val bottomNav = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#111111"))
                setPadding(20, 30, 20, 30)
                gravity = android.view.Gravity.CENTER
            }

            btnPhotos = TextView(this).apply {
                text = "Photos"
                textSize = 18f
                setTextColor(Color.WHITE)
                setPadding(50, 10, 50, 10)
                setOnClickListener {
                    isAlbumMode = false
                    btnPhotos.setTextColor(Color.parseColor("#03dac6"))
                    btnAlbums.setTextColor(Color.WHITE)
                    showPhotos()
                }
            }

            btnAlbums = TextView(this).apply {
                text = "Albums"
                textSize = 18f
                setTextColor(Color.WHITE)
                setPadding(50, 10, 50, 10)
                setOnClickListener {
                    isAlbumMode = true
                    btnAlbums.setTextColor(Color.parseColor("#03dac6"))
                    btnPhotos.setTextColor(Color.WHITE)
                    showAlbums()
                }
            }

            bottomNav.addView(btnPhotos)
            bottomNav.addView(btnAlbums)
            rootLayout.addView(bottomNav)

            setContentView(rootLayout)
            btnPhotos.setTextColor(Color.parseColor("#03dac6"))

            checkAndRequestPermissions()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestPermissions() {
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
            loadMedia()
        }
    }

    private fun loadMedia() {
        allPhotos.clear()
        albumMap.clear()
        albumNames.clear()

        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                val index = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                while (it.moveToNext()) {
                    val path = it.getString(index)
                    if (path != null && File(path).exists()) {
                        allPhotos.add(path)
                        val folder = File(path).parentFile?.name ?: "Unknown"
                        if (!albumMap.containsKey(folder)) {
                            albumMap[folder] = ArrayList()
                            albumNames.add(folder)
                        }
                        albumMap[folder]?.add(path)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        showPhotos()
    }

    private fun showPhotos() {
        if (allPhotos.isNotEmpty()) {
            recyclerView.layoutManager = GridLayoutManager(this, 4)
            recyclerView.adapter = MediaAdapter(allPhotos) { path ->
                // Full Screen View Intent
                val intent = Intent(this, FullScreenActivity::class.java).apply {
                    putExtra("path", path)
                }
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "No photos found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAlbums() {
        if (albumNames.isNotEmpty()) {
            recyclerView.layoutManager = GridLayoutManager(this, 1)
            recyclerView.adapter = AlbumAdapter(albumNames) { folderName ->
                val photosInAlbum = albumMap[folderName] ?: ArrayList()
                recyclerView.layoutManager = GridLayoutManager(this, 4)
                recyclerView.adapter = MediaAdapter(photosInAlbum) { path ->
                    val intent = Intent(this, FullScreenActivity::class.java).apply {
                        putExtra("path", path)
                    }
                    startActivity(intent)
                }
            }
        } else {
            Toast.makeText(this, "No albums found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            loadMedia()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Media Grid Adapter
    class MediaAdapter(private val list: List<String>, private val onClick: (String) -> Unit) : RecyclerView.Adapter<MediaAdapter.VH>() {
        class VH(val img: ImageView) : RecyclerView.ViewHolder(img)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val img = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 250)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(2, 2, 2, 2)
            }
            return VH(img)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val path = list[position]
            Glide.with(holder.img.context).load(File(path)).centerCrop().into(holder.img)
            holder.itemView.setOnClickListener { onClick(path) }
        }

        override fun getItemCount() = list.size
    }

    // Album List Adapter
    class AlbumAdapter(private val list: List<String>, private val onClick: (String) -> Unit) : RecyclerView.Adapter<AlbumAdapter.VH>() {
        class VH(val txt: TextView) : RecyclerView.ViewHolder(txt)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val txt = TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(40, 40, 40, 40)
                textSize = 18f
                setTextColor(Color.WHITE)
            }
            return VH(txt)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val name = list[position]
            holder.txt.text = "📁  $name"
            holder.itemView.setOnClickListener { onClick(name) }
        }

        override fun getItemCount() = list.size
    }
}
