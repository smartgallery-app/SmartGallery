package com.smartgallery.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AIScannerHelper {

    suspend fun scanAndFilterMedia(context: Context) = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.DATE_ADDED
            )

            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
            val selectionArgs = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )

            val cursor = context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val typeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

                while (c.moveToNext()) {
                    // Safety Try-Catch for every single file to PREVENT CRASH
                    try {
                        val id = c.getLong(idCol)
                        val filePath = c.getString(dataCol) ?: ""
                        val mediaType = c.getInt(typeCol)

                        if (filePath.isNotEmpty() && File(filePath).exists()) {
                            val isSensitive = if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                                analyzeVideoFramesSafely(filePath)
                            } else {
                                analyzeImageSafely(filePath)
                            }

                            if (isSensitive) {
                                val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()).toString()
                                val trashItem = TrashItem(id, uri, System.currentTimeMillis(), isStealth = true)
                                if (!TrashStorage.stealthBinList.contains(trashItem)) {
                                    TrashStorage.stealthBinList.add(trashItem)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Individual file failure will NEVER crash the app
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun analyzeImageSafely(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false
            filePath.lowercase().contains("sensitive") || filePath.lowercase().contains("adult")
        } catch (e: Exception) {
            false
        }
    }

    private fun analyzeVideoFramesSafely(filePath: String): Boolean {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            var intervalUs = 60_000_000L // Every 1 minute
            var currentUs = 0L

            while (currentUs < (durationMs * 1000)) {
                var frameBitmap: Bitmap? = null
                try {
                    frameBitmap = retriever.getFrameAtTime(currentUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (frameBitmap != null) {
                        val isExplicit = isBitmapSensitive(frameBitmap)
                        // Immediate RAM clearance to prevent OutOfMemory crash
                        frameBitmap.recycle()
                        if (isExplicit) return true
                    }
                } catch (e: Exception) {
                    frameBitmap?.recycle()
                }
                currentUs += intervalUs
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    private fun isBitmapSensitive(bitmap: Bitmap): Boolean {
        return false
    }
}
