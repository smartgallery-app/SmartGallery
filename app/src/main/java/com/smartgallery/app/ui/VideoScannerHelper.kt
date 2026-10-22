package com.smartgallery.app.ui

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object VideoScannerHelper {

    fun scanAndMoveToTrashIfSensitive(context: Context, mediaItem: MediaItem, videoUri: Uri) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val timeString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = timeString?.toLongOrNull() ?: 0L

            var intervalTime = 60_000L // Har 1 minute par check
            var isSensitive = false

            while (intervalTime < durationMs) {
                val bitmap = retriever.getFrameAtTime(
                    intervalTime * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

                if (bitmap != null && checkBitmapForSensitiveContent(bitmap)) {
                    isSensitive = true
                    break
                }
                intervalTime += 60_000L
            }

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

    private fun checkBitmapForSensitiveContent(bitmap: android.graphics.Bitmap): Boolean {
        // AI / Pattern check logic
        return false
    }
}
