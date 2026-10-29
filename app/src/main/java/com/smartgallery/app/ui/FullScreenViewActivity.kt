package com.smartgallery.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

        val mediaUri = intent.getStringExtra("media_uri")

        // Load single image into ViewPager using a simple adapter or direct loading view
        if (!mediaUri.isNullOrEmpty()) {
            val adapter = FullScreenAdapter(listOf(mediaUri)) {
                // Click to finish or toggle UI bars if needed
            }
            viewPager.adapter = adapter
        }

        btnInfo?.setOnClickListener {
            if (infoTextView.visibility == View.VISIBLE) {
                infoTextView.visibility = View.GONE
            } else {
                infoTextView.visibility = View.VISIBLE
                infoTextView.text = "URI: $mediaUri"
            }
        }
    }
}

// Simple internal adapter for FullScreen ViewPager2
class FullScreenAdapter(
    private val uriList: List<String>,
    private val onClick: () -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<FullScreenAdapter.ViewHolder>() {

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewFullScreenItem)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fullscreen_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = uriList[position]
        Glide.with(holder.imageView.context)
            .load(uri)
            .into(holder.imageView)

        holder.imageView.setOnClickListener {
            onClick()
        }
    }

    override fun getItemCount(): Int = uriList.size
}
