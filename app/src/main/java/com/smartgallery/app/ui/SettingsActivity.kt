package com.smartgallery.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_settings)
            
            // Back button binding matching XML id: btnSettingsBack
            val btnBack = findViewById<View>(R.id.btnSettingsBack) 
                ?: findViewById<View>(R.id.btnBack)
            btnBack?.setOnClickListener { 
                finish() 
            }

            // Stealth Recovery Box click listener matching XML id: btnOpenStealthBox
            val btnStealthBox = findViewById<View>(R.id.btnOpenStealthBox)
            btnStealthBox?.setOnClickListener {
                val intent = Intent(this, TrashActivity::class.java)
                startActivity(intent)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}
