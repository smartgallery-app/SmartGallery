package com.smartgallery.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnOpenStealthTrash = findViewById<Button>(R.id.btnOpenStealthTrash)

        btnOpenStealthTrash.setOnClickListener {
            val intent = Intent(this, StealthTrashActivity::class.java)
            startActivity(intent)
        }
    }
}
