package com.smartgallery.app

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

class MediaFilterHelper(private val context: Context) {

    data class MediaItem(
        val uri: String,
        val isVideo: Boolean = false,
        var isFlaggedForDeletion: Boolean = false
    )

    // Video frame analysis interval: 4 seconds (3 se 5 second ke beech)
    private val SCAN_INTERVAL_MS = 4000L

    /**
     * Photo scan karne ka function
     */
    fun scanImage(imagePath: String): Boolean {
        // AI Model / Logic ke dwaara image check karna
        val isUnsafe = checkNsfwContent(imagePath)
        if (isUnsafe) {
            deleteMediaFile(imagePath)
        }
        return isUnsafe
    }

    /**
     * Video scan karne ka function (Har 3-5 second ka frame check karega)
     */
    fun scanVideo(videoPath: String): Boolean {
        val retriever = MediaMetadataRetriever()
        var isVideoUnsafe = false

        try {
            retriever.setDataSource(videoPath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            var currentPositionMs = 0L

            // Har 3-5 second ke interval par frame nikal kar check karega
            while (currentPositionMs < durationMs) {
                val frameBitmap = retriever.getFrameAtTime(
                    currentPositionMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

                if (frameBitmap != null && isFrameUnsafe(frameBitmap)) {
                    isVideoUnsafe = true
                    break // Agar ek bhi ashlil frame mil gaya toh scan wahi rok do
                }

                currentPositionMs += SCAN_INTERVAL_MS
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }

        // Agar video mein inappropriate content milta hai toh video delete kar do
        if (isVideoUnsafe) {
            deleteMediaFile(videoPath)
        }

        return isVideoUnsafe
    }

    private fun checkNsfwContent(filePath: String): Boolean {
        // AI Model Detection Logic (TFLite integration point)
        return false
    }

    private fun isFrameUnsafe(bitmap: Bitmap): Boolean {
        // AI Frame Scanning Logic
        return false
    }

    /**
     * Photo ya Video ko delete karne ka logic
     */
    private fun deleteMediaFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
