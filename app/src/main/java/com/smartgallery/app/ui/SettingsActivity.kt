package com.smartgallery.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.smartgallery.app.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<ImageButton>(R.id.btnBackSettings)
        val btnStealthBox = findViewById<LinearLayout>(R.id.btnStealthBox)
        val switchPrivate = findViewById<SwitchCompat>(R.id.switchShowPrivate)
        val switchAutoplay = findViewById<SwitchCompat>(R.id.switchAutoplay)

        btnBack?.setOnClickListener {
            finish()
        }

        btnStealthBox?.setOnClickListener {
            startActivity(Intent(this, StealthTrashActivity::class.java))
        }

        switchPrivate?.setOnCheckedChangeListener { _, isChecked ->
            // Settings logic for private visibility
        }

        switchAutoplay?.setOnCheckedChangeListener { _, isChecked ->
            // Settings logic for autoplay
        }
    }
}
