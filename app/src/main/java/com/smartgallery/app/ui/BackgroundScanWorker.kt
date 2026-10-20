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
            MediaStore.MediaColumns.MIME_TYPE
        )

        val uri = MediaStore.Files.getContentUri("external")
        val cursor = context.contentResolver.query(
            uri, projection, null, null, null
        )

        cursor?.use { c ->
            val pathColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val mimeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

            while (c.moveToNext()) {
                val path = c.getString(pathColumn)
                val mimeType = c.getString(mimeColumn) ?: ""
                val mediaItem = MediaItem(path, "Scanned File")

                if (mimeType.startsWith("video/")) {
                    val videoUri = Uri.parse(path)
                    VideoScannerHelper.scanAndMoveToTrashIfSensitive(context, mediaItem, videoUri)
                } else if (mimeType.startsWith("image/")) {
                    val isSensitivePhoto = scanPhotoForSafety(path)
                    if (isSensitivePhoto) {
                        if (!TrashStorage.binList.contains(mediaItem)) {
                            TrashStorage.binList.add(mediaItem)
                        }
                    }
                }
            }
        }
    }

    private fun scanPhotoForSafety(filePath: String): Boolean {
        return false
    }
}
