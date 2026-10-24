package com.smartgallery.app.ui

data class TrashItem(
    val id: Long,
    val uri: String,
    val deletedTime: Long,
    val isStealth: Boolean = false
) {
    fun getRemainingHours(): Long {
        val elapsedTime = System.currentTimeMillis() - deletedTime
        val remainingMs = (12 * 60 * 60 * 1000) - elapsedTime
        return if (remainingMs > 0) remainingMs / (1000 * 60 * 60) else 0
    }

    fun getRemainingDays(): Long {
        val elapsedTime = System.currentTimeMillis() - deletedTime
        val remainingMs = (30L * 24 * 60 * 60 * 1000) - elapsedTime
        return if (remainingMs > 0) remainingMs / (1000 * 60 * 60 * 24) else 0
    }
}

object TrashStorage {
    val mainBinList = mutableListOf<TrashItem>()
    val trashBinList = mutableListOf<TrashItem>()
    val stealthBinList = mutableListOf<TrashItem>()
}
