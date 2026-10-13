package com.smartgallery.ai

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

class MediaRepository(private val context: Context, private val database: AppDatabase) {

    fun getAllMedia(): List<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()

        // Load Images
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )

        val cursor = context.contentResolver.query(imageUri, imageProjection, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn) ?: "Image"
                val dateAdded = it.getLong(dateColumn)
                val uri = ContentUris.withAppendedId(imageUri, id)

                mediaList.add(MediaItem(id, uri, name, false, dateAdded))
            }
        }

        // Load Videos
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED
        )

        val videoCursor = context.contentResolver.query(videoUri, videoProjection, null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC")
        videoCursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn) ?: "Video"
                val dateAdded = it.getLong(dateColumn)
                val uri = ContentUris.withAppendedId(videoUri, id)

                mediaList.add(MediaItem(id, uri, name, true, dateAdded))
            }
        }

        return mediaList.sortedByDescending { it.dateAdded }
    }

    // Move item to Trash Bin (Database Record + 7-Day Expiry Timer)
    suspend fun moveToTrash(mediaItem: MediaItem, isAiFlagged: Boolean = false) {
        val expiryDays = if (isAiFlagged) 1 else 7
        val expiryTime = System.currentTimeMillis() + (expiryDays * 24 * 60 * 60 * 1000L)
        
        val deletedEntity = DeletedItemEntity(
            originalPath = mediaItem.uri.toString(),
            deletedTimestamp = System.currentTimeMillis(),
            expiryTimestamp = expiryTime,
            isAiFlagged = isAiFlagged
        )
        database.deletedItemDao().insertDeletedItem(deletedEntity)
    }

    // Restore File Back to Original Gallery View
    suspend fun restoreItem(deletedEntity: DeletedItemEntity) {
        database.deletedItemDao().deleteItemByPath(deletedEntity.originalPath)
    }

    // Permanent Hard Delete via Android MediaStore
    fun deletePermanently(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val resolver: ContentResolver = context.contentResolver
            val deletedRows = resolver.delete(uri, null, null)
            deletedRows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
