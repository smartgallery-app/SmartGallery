package com.smartgallery.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.smartgallery.app.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<ImageButton>(R.id.btnSettingsBack)
        val switchPrivate = findViewById<SwitchCompat>(R.id.switchShowPrivate)
        val switchAutoplay = findViewById<SwitchCompat>(R.id.switchAutoplay)
        val btnStealthBox = findViewById<RelativeLayout>(R.id.btnStealthRecoveryBox)
        val btnSortOrder = findViewById<RelativeLayout>(R.id.btnSortOrder)
        val tvSortValue = findViewById<TextView>(R.id.tvSortValue)

        val prefs = getSharedPreferences("SmartGalleryPrefs", Context.MODE_PRIVATE)

        btnBack?.setOnClickListener { finish() }

        switchPrivate?.isChecked = prefs.getBoolean("show_private_album", true)
        switchAutoplay?.isChecked = prefs.getBoolean("autoplay_videos", true)
        tvSortValue?.text = prefs.getString("sort_order", "Newest at top")

        switchPrivate?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("show_private_album", isChecked).apply()
            Toast.makeText(this, if (isChecked) "Private album enabled" else "Private album hidden", Toast.LENGTH_SHORT).show()
        }

        switchAutoplay?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("autoplay_videos", isChecked).apply()
        }

        btnSortOrder?.setOnClickListener {
            val current = tvSortValue?.text.toString()
            val newOrder = if (current == "Newest at top") "Oldest at top" else "Newest at top"
            tvSortValue?.text = newOrder
            prefs.edit().putString("sort_order", newOrder).apply()
        }

        btnStealthBox?.setOnClickListener {
            startActivity(Intent(this, StealthTrashActivity::class.java))
        }
    }
}
