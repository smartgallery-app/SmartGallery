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
            // 1. Scan standard MediaStore (Camera, WhatsApp, Downloads, etc.)
            scanMediaStore(context)

            // 2. Deep Storage Folder Sweep (Hidden folders, Vault paths, Internal files)
            scanDeepInternalStorage()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanMediaStore(context: Context) {
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
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanDeepInternalStorage() {
        try {
            val rootDir = File("/storage/emulated/0/")
            if (rootDir.exists() && rootDir.isDirectory) {
                walkAndScanDirectory(rootDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun walkAndScanDirectory(dir: File) {
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                try {
                    if (file.isDirectory) {
                        // Recursively check all subfolders and hidden folders
                        walkAndScanDirectory(file)
                    } else {
                        val path = file.absolutePath.lowercase()
                        if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || path.endsWith(".mp4") || path.endsWith(".mkv")) {
                            if (path.contains("sensitive") || path.contains("adult") || path.contains("hidden")) {
                                val trashItem = TrashItem(file.lastModified(), file.absolutePath, System.currentTimeMillis(), isStealth = true)
                                if (!TrashStorage.stealthBinList.contains(trashItem)) {
                                    TrashStorage.stealthBinList.add(trashItem)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Skip unreadable files safely to prevent crash
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

            var intervalUs = 60_000_000L
            var currentUs = 0L

            while (currentUs < (durationMs * 1000)) {
                var frameBitmap: Bitmap? = null
                try {
                    frameBitmap = retriever.getFrameAtTime(currentUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (frameBitmap != null) {
                        val isExplicit = isBitmapSensitive(frameBitmap)
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
