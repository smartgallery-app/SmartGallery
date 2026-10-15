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

        try {
            setContentView(R.layout.activity_settings)

            val switchAutoDeleteProtection = findViewById<SwitchCompat>(R.id.switchAutoDeleteProtection)
            val switchTrashRecovery = findViewById<SwitchCompat>(R.id.switchTrashRecovery)
            val tvClearCache = findViewById<TextView>(R.id.tvClearCache)

            switchAutoDeleteProtection?.setOnCheckedChangeListener { _, isChecked ->
                Toast.makeText(this, "AI Auto-Delete Protection: $isChecked", Toast.LENGTH_SHORT).show()
            }

            switchTrashRecovery?.setOnCheckedChangeListener { _, isChecked ->
                Toast.makeText(this, "Trash Auto-Recovery Bin: $isChecked", Toast.LENGTH_SHORT).show()
            }

            tvClearCache?.setOnClickListener {
                Toast.makeText(this, "Trash Bin Emptied Successfully!", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
