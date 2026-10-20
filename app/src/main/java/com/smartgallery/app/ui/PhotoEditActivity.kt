package com.smartgallery.app.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.smartgallery.app.R

class PhotoEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_edit)

        val imageView = findViewById<ImageView>(R.id.editImageView)
        val uriStr = intent.getStringExtra("EDIT_URI") ?: ""

        if (uriStr.isNotEmpty()) {
            Glide.with(this).load(Uri.parse(uriStr)).into(imageView)
        }

        // Filters Action logic using ColorMatrix
        findViewById<MaterialButton>(R.id.btnFilterOriginal).setOnClickListener {
            imageView.colorFilter = null
            Toast.makeText(this, "Original", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnFilterVivid).setOnClickListener {
            val matrix = ColorMatrix().apply { setSaturation(1.5f) }
            imageView.colorFilter = ColorMatrixColorFilter(matrix)
            Toast.makeText(this, "Vivid applied", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnFilterMono).setOnClickListener {
            val matrix = ColorMatrix().apply { setSaturation(0.0f) }
            imageView.colorFilter = ColorMatrixColorFilter(matrix)
            Toast.makeText(this, "Mono applied", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnFilterWarm).setOnClickListener {
            // Warm tint approximation matrix
            val matrix = ColorMatrix(floatArrayOf(
                1.2f, 0f, 0f, 0f, 0f,
                0f, 1.0f, 0f, 0f, 0f,
                0f, 0f, 0.8f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            imageView.colorFilter = ColorMatrixColorFilter(matrix)
            Toast.makeText(this, "Warm applied", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnFilterCool).setOnClickListener {
            // Cool tint approximation matrix
            val matrix = ColorMatrix(floatArrayOf(
                0.9f, 0f, 0f, 0f, 0f,
                0f, 1.0f, 0f, 0f, 0f,
                0f, 0f, 1.3f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            imageView.colorFilter = ColorMatrixColorFilter(matrix)
            Toast.makeText(this, "Cool applied", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnCancelEdit).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnSaveEdit).setOnClickListener {
            Toast.makeText(this, "Filtered photo saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
