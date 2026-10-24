package com.smartgallery.app.ui

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_settings)
            val btnBack = findViewById<ImageButton>(R.id.btnBackSettings) ?: findViewById(R.id.btnBack)
            btnBack?.setOnClickListener { finish() }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}
