package com.smartgallery.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever

class NSFWClassifier(private val context: Context) {

    // Frame & Bitmap Skin-Exposure & Sensitive Pixel Analysis
    fun analyzeBitmap(bitmap: Bitmap): Boolean {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            var skinTonePixelCount = 0
            var sampledCount = 0

            for (x in 0 until width step 4) {
                for (y in 0 until height step 4) {
                    val pixel = bitmap.getPixel(x, y)
                    val red = (pixel shr 16) and 0xff
                    val green = (pixel shr 8) and 0xff
                    val blue = pixel and 0xff

                    if (red > 95 && green > 40 && blue > 20 &&
                        (Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue))) > 15 &&
                        Math.abs(red - green) > 15 && red > green && red > blue) {
                        skinTonePixelCount++
                    }
                    sampledCount++
                }
            }

            val skinRatio = if (sampledCount > 0) skinTonePixelCount.toFloat() / sampledCount else 0f
            skinRatio > 0.60f
        } catch (e: Exception) {
            false
        }
    }

    // Video Frame Extractor & Multi-Point Sampling Engine
    fun analyzeVideoFrames(videoPath: String): Boolean {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLong() ?: 0L

            val checkIntervals = arrayOf(duration * 0.20, duration * 0.50, duration * 0.80)
            
            for (timeUs in checkIntervals) {
                val bitmap = retriever.getFrameAtTime(timeUs.toLong() * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null && analyzeBitmap(bitmap)) {
                    retriever.release()
                    return true
                }
            }
            retriever.release()
            false
        } catch (e: Exception) {
            false
        }
    }
}
