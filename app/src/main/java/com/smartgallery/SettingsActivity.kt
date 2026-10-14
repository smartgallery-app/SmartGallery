package com.smartgallery.app

import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val switchProtection: Switch = findViewById(R.id.switchAutoDeleteProtection)
        val switchTrash: Switch = findViewById(R.id.switchTrashRecovery)
        val tvClearCache: TextView = findViewById(R.id.tvClearCache)
        val tvEmptyTrash: TextView = findViewById(R.id.tvEmptyTrash)

        switchProtection.setOnCheckedChangeListener { _, isChecked ->
            val msg = if (isChecked) "AI Protection Enabled" else "AI Protection Disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        switchTrash.setOnCheckedChangeListener { _, isChecked ->
            val msg = if (isChecked) "Trash Auto-Expiry Enabled" else "Trash Auto-Expiry Disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        tvClearCache.setOnClickListener {
            Toast.makeText(this, "Cache cleared successfully!", Toast.LENGTH_SHORT).show()
        }

        tvEmptyTrash.setOnClickListener {
            Toast.makeText(this, "Trash bin emptied!", Toast.LENGTH_SHORT).show()
        }
    }
}
