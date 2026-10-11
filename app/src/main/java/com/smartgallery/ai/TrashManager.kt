package com.smartgallery.ai

import java.io.File

object TrashManager {

    // 7 Days in milliseconds for general media
    private const val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L

    // 24 Hours in milliseconds for explicit media
    private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L

    suspend fun cleanupExpiredTrash(trashDao: TrashDao) {
        val currentTime = System.currentTimeMillis()
        val allItems = trashDao.getAllTrashItems()

        for (item in allItems) {
            val expiryTime = if (item.isExplicit) {
                item.deletedAt + TWENTY_FOUR_HOURS_MS
            } else {
                item.deletedAt + SEVEN_DAYS_MS
            }

            if (currentTime >= expiryTime) {
                // Delete physical file locally
                val file = File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
                // Remove entry from local database
                trashDao.deleteTrashItem(item)
            }
        }
    }
}
