package com.smartgallery.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.smartgallery.app.R

class FullScreenViewActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var infoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        viewPager = findViewById(R.id.viewPager)
        infoTextView = findViewById(R.id.infoTextView)

        val btnShare = findViewById<MaterialButton>(R.id.btnShare)
        val btnFavorite = findViewById<MaterialButton>(R.id.btnFavorite)
        val btnEdit = findViewById<MaterialButton>(R.id.btnEdit)
        val btnDelete = findViewById<MaterialButton>(R.id.btnDelete)
        val btnInfo = findViewById<MaterialButton>(R.id.btnInfo)

        // Intent se media URI fetch kar rahe hain
        val mediaUri = intent.getStringExtra("media_uri") ?: ""

        val mediaList = if (mediaUri.isNotEmpty()) listOf(mediaUri) else listOf("")
        val adapter = FullScreenPagerAdapter(mediaList) {
            // Tap karne par activity finish ya UI toggle kar sakte hain
        }
        viewPager.adapter = adapter

        // 1. Share Feature
        btnShare?.setOnClickListener {
            if (mediaUri.isNotEmpty()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(mediaUri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Image via"))
            } else {
                Toast.makeText(this, "Media not available", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Favorite Feature
        btnFavorite?.setOnClickListener {
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show()
        }

        // 3. Edit Feature
        btnEdit?.setOnClickListener {
            Toast.makeText(this, "Opening Editor...", Toast.LENGTH_SHORT).show()
        }

        // 4. Delete Feature
        btnDelete?.setOnClickListener {
            Toast.makeText(this, "Moved to Recovery Box", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 5. Info Overlay Toggle
        btnInfo?.setOnClickListener {
            if (infoTextView != null) {
                if (infoTextView.visibility == View.VISIBLE) {
                    infoTextView.visibility = View.GONE
                } else {
                    infoTextView.visibility = View.VISIBLE
                    infoTextView.text = "File URI:\n$mediaUri"
                }
            }
        }
    }
}

// Inner ViewPager Adapter for smooth swiping and image loading
class FullScreenPagerAdapter(
    private val uriList: List<String>,
    private val onClick: () -> Unit
) : RecyclerView.Adapter<FullScreenPagerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewFullScreenItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fullscreen_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = uriList[position]
        if (uri.isNotEmpty()) {
            Glide.with(holder.imageView.context)
                .load(uri)
                .into(holder.imageView)
        }

        holder.imageView.setOnClickListener {
            onClick()
        }
    }

    override fun getItemCount(): Int = uriList.size
}
