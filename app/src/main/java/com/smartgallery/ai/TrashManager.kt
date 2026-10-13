package com.smartgallery.ai

import android.content.Context

class TrashManager(private val database: AppDatabase) {

    suspend fun getAllTrashItems(): List<TrashEntity> {
        return database.trashDao().getAllDeletedItems()
    }

    suspend fun deleteTrashItem(uri: String) {
        database.trashDao().deleteItemByPath(uri)
    }
}
