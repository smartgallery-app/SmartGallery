package com.smartgallery.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
        setContentView(R.layout.activity_full_screen) // Aapka wala exact XML layout

        viewPager = findViewById(R.id.viewPager)
        infoTextView = findViewById(R.id.infoTextView)

        val btnShare = findViewById<MaterialButton>(R.id.btnShare)
        val btnFavorite = findViewById<MaterialButton>(R.id.btnFavorite)
        val btnEdit = findViewById<MaterialButton>(R.id.btnEdit)
        val btnDelete = findViewById<MaterialButton>(R.id.btnDelete)
        val btnInfo = findViewById<MaterialButton>(R.id.btnInfo)

        val mediaUri = intent.getStringExtra("media_uri") ?: ""

        // ViewPager2 ke liye list set kar rahe hain taaki swiping bhi chale aur error bhi na aaye
        val mediaList = listOf(mediaUri)
        val adapter = FullScreenPagerAdapter(mediaList) {
            // Tap karne par action handle kar sakte hain
        }
        viewPager.adapter = adapter

        // Action Buttons Click Listeners (Features safe hain)
        btnShare?.setOnClickListener {
            // Share logic
        }

        btnFavorite?.setOnClickListener {
            // Favorite logic
        }

        btnEdit?.setOnClickListener {
            // Edit logic
        }

        btnDelete?.setOnClickListener {
            // Delete logic
        }

        btnInfo?.setOnClickListener {
            if (infoTextView != null) {
                if (infoTextView.visibility == View.VISIBLE) {
                    infoTextView.visibility = View.GONE
                } else {
                    infoTextView.visibility = View.VISIBLE
                    infoTextView.text = "URI: $mediaUri"
                }
            }
        }
    }
}

// FullScreen ViewPager Adapter
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
        Glide.with(holder.imageView.context)
            .load(uri)
            .into(holder.imageView)

        holder.imageView.setOnClickListener {
            onClick()
        }
    }

    override fun getItemCount(): Int = uriList.size
}
