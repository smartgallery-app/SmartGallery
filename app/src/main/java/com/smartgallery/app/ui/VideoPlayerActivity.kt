package com.smartgallery.app.ui

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.smartgallery.app.R

class VideoPlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_video_player)

            val videoView = findViewById<VideoView>(R.id.videoView)
            val mediaUriStr = intent.getStringExtra("MEDIA_URI") ?: ""

            if (mediaUriStr.isNotEmpty()) {
                val mediaController = MediaController(this)
                mediaController.setAnchorView(videoView)
                videoView.setMediaController(mediaController)

                videoView.setVideoURI(Uri.parse(mediaUriStr))
                videoView.requestFocus()
                videoView.start()
            } else {
                Toast.makeText(this, "Video cannot be played", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
