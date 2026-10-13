package com.smartgallery.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

class VideoFrameSampler(private val context: Context) {

    /**
     * Video ko 1-1 minute (60000 ms) ke gap par bhaga-bhaga kar frames extract karega.
     */
    fun extractFramesAtIntervals(videoUri: Uri, intervalMs: Long = 60000L): List<Bitmap> {
        val frameList = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, videoUri)
            
            // Video ki total duration nikalte hain (milliseconds mein)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            var currentTimeMs: Long = 0L

            // Har 1 minute ke gap par frames capture karenge
            while (currentTimeMs <= durationMs) {
                // MediaMetadataRetriever microsecond leta hai isliye * 1000 kiya
                val bitmap = retriever.getFrameAtTime(currentTimeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                
                if (bitmap != null) {
                    frameList.add(bitmap)
                }

                currentTimeMs += intervalMs
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }

        return frameList
    }
}
