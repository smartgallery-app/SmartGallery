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
            val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val pathColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val mimeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val path = c.getString(pathColumn) ?: ""
                val mimeType = c.getString(mimeColumn) ?: ""

                // String path format for MediaItem
                val mediaItem = MediaItem(id, path, "Scanned File")

                if (mimeType.startsWith("video/")) {
                    val videoUri = Uri.parse(path)
                    VideoScannerHelper.scanAndMoveToTrashIfSensitive(context, mediaItem, videoUri)
                } else if (mimeType.startsWith("image/")) {
                    // Safe photo check simulation
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
