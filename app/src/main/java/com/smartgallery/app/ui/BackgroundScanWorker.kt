package com.smartgallery.app.ui

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.work.Worker
import androidx.work.WorkerParameters

class BackgroundScanWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        scanAllGalleryMedia()
        return Result.success()
    }

    private fun scanAllGalleryMedia() {
        val context = applicationContext
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

        val uri = MediaStore.Files.getContentUri("external")
        val cursor = context.contentResolver.query(
            uri, projection, null, null, null
        )

        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val pathColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val mimeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val bucketColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val path = c.getString(pathColumn) ?: ""
                val mimeType = c.getString(mimeColumn) ?: ""
                val dateAdded = c.getLong(dateColumn)
                val bucketName = c.getString(bucketColumn) ?: "Gallery"

                // Pass all 5 parameters as required by your MediaItem data class
                val mediaItem = MediaItem(
                    id = id,
                    path = path,
                    name = "Scanned File",
                    dateAdded = dateAdded,
                    bucketName = bucketName
                )

                if (mimeType.startsWith("video/")) {
                    val videoUri = Uri.parse(path)
                    VideoScannerHelper.scanAndMoveToTrashIfSensitive(context, mediaItem, videoUri)
                } else if (mimeType.startsWith("image/")) {
                    val isSensitivePhoto = false
                    if (isSensitivePhoto) {
                        if (!TrashStorage.binList.contains(mediaItem)) {
                            TrashStorage.binList.add(mediaItem)
                        }
                    }
                }
            }
        }
    }
}
