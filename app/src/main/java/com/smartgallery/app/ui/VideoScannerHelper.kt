package com.smartgallery.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

object VideoScannerHelper {

    /**
     * Har video ko scan karega - chahe video kitni bhi lambi ho.
     * Har 1 minute ke interval par 4-5 second ka frame snapshot lekar ashlilta check karega.
     */
    fun scanAndMoveToTrashIfSensitive(context: Context, mediaItem: MediaItem, videoUri: Uri) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = timeString?.toLongOrNull() ?: 0L

            var intervalTime = 60_000L // Har 1 minute (60 sec) par check
            var isSensitive = false

            while (intervalTime < durationMs) {
                // Har minute par frame ka preview snapshot nikalna
                val bitmap = retriever.getFrameAtTime(
                    intervalTime * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

                if (bitmap != null && checkBitmapForSensitiveContent(bitmap)) {
                    isSensitive = true
                    break // Flagged! Aage scan ki zaroorat nahi
                }

                intervalTime += 60_000L // Next 1 minute
            }

            // Flagged hote hi seedha 12-Hour Stealth Recovery Box mein shift
            if (isSensitive) {
                if (!TrashStorage.stealthBinList.contains(mediaItem)) {
                    TrashStorage.stealthBinList.add(mediaItem)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
    }

    private fun checkBitmapForSensitiveContent(bitmap: Bitmap): Boolean {
        // AI Model / Pattern matching logic for adult content detection
        return AIScannerHelper.analyzeFrameForAdultContent(bitmap)
    }
}
