package com.smartgallery.app.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.smartgallery.app.R

class FullScreenViewActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var infoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen) // Aapka wala XML layout

        viewPager = findViewById(R.id.viewPager)
        infoTextView = findViewById(R.id.infoTextView)

        val btnShare = findViewById<MaterialButton>(R.id.btnShare)
        val btnFavorite = findViewById<MaterialButton>(R.id.btnFavorite)
        val btnEdit = findViewById<MaterialButton>(R.id.btnEdit)
        val btnDelete = findViewById<MaterialButton>(R.id.btnDelete)
        val btnInfo = findViewById<MaterialButton>(R.id.btnInfo)

        val mediaUri = intent.getStringExtra("media_uri")

        // Agar aap chahein toh yahan buttons ke click listeners laga sakte hain
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
            if (infoTextView.visibility == View.VISIBLE) {
                infoTextView.visibility = View.GONE
            } else {
                infoTextView.visibility = View.VISIBLE
                infoTextView.text = "URI: $mediaUri"
            }
        }
    }
}
