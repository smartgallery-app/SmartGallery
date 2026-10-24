package com.smartgallery.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_settings)
            
            val btnBack = findViewById<View>(R.id.btnBackSettings) 
                ?: findViewById<View>(R.id.btnBack)
                
            btnBack?.setOnClickListener { finish() }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
}
