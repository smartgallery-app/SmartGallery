package com.smartgallery.app.trash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R

class TrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = "Trash / Recovery Box"
    }
}
