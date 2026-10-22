package com.smartgallery.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Back button
        val btnBack = findViewById<ImageButton>(R.id.btnSettingsBack)
        btnBack?.setOnClickListener {
            finish()
        }

        // Stealth Recovery Box Click Action
        val btnOpenStealthBox = findViewById<RelativeLayout>(R.id.btnOpenStealthBox)
        btnOpenStealthBox?.setOnClickListener {
            val intent = Intent(this, StealthTrashActivity::class.java)
            startActivity(intent)
        }
    }
}
