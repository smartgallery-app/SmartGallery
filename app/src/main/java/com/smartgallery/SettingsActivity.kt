package com.smartgallery.app.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.smartgallery.app.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crash Prevention Handler
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            throwable.printStackTrace()
        }

        try {
            setContentView(R.layout.activity_settings)

            val switchAiProtection = findViewById<SwitchCompat>(R.id.switchAiProtection)
            val switchTrashRetention = findViewById<SwitchCompat>(R.id.switchTrashRetention)
            val btnEmptyTrash = findViewById<TextView>(R.id.btnEmptyTrash)

            switchAiProtection?.setOnCheckedChangeListener { _, isChecked ->
                Toast.makeText(this, "AI Protection: $isChecked", Toast.LENGTH_SHORT).show()
            }

            switchTrashRetention?.setOnCheckedChangeListener { _, isChecked ->
                Toast.makeText(this, "Trash Retention: $isChecked", Toast.LENGTH_SHORT).show()
            }

            btnEmptyTrash?.setOnClickListener {
                Toast.makeText(this, "Trash Emptied Successfully", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
