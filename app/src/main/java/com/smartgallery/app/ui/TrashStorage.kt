package com.smartgallery.app.ui

object TrashStorage {
    val mainBinList = mutableListOf<TrashItem>()
    val trashBinList = mutableListOf<TrashItem>()
    val stealthBinList = mutableListOf<TrashItem>()

    fun getTrashItems(): List<TrashItem> {
        return trashBinList.ifEmpty { mainBinList }
    }

    fun clearAll() {
        mainBinList.clear()
        trashBinList.clear()
        stealthBinList.clear()
    }
}

data class TrashItem(
    val id: String = "",
    val uri: String = "",
    val path: String = "",
    val deletedTime: Long = System.currentTimeMillis(),
    val isStealth: Boolean = false
) {
    fun getRemainingHours(): Long {
        val elapsedTime = System.currentTimeMillis() - deletedTime
        val remainingMs = (12L * 60 * 60 * 1000L) - elapsedTime
        return if (remainingMs > 0) remainingMs / (1000 * 60 * 60) else 0
    }

    fun getRemainingDays(): Long {
        val elapsedTime = System.currentTimeMillis() - deletedTime
        val remainingMs = (30L * 24 * 60 * 60 * 1000L) - elapsedTime
        return if (remainingMs > 0) remainingMs / (1000 * 60 * 60 * 24) else 0
    }
}
