package com.smartgallery.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        }

        try {
            setContentView(R.layout.activity_main)

            val btnSettings = findViewById<ImageView>(R.id.btnSettings)
            btnSettings.setOnClickListener {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
