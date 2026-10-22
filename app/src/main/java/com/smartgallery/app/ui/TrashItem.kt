package com.smartgallery.app.ui

import java.util.concurrent.TimeUnit

data class TrashItem(
    val id: Long,
    val uri: String,
    val timestamp: Long,
    val isStealth: Boolean = false
) {
    fun getRemainingDays(): Long {
        val millisLeft = (30L * 24 * 60 * 60 * 1000) - (System.currentTimeMillis() - timestamp)
        val days = TimeUnit.MILLISECONDS.toDays(millisLeft)
        return if (days > 0) days else 0
    }

    fun getRemainingHours(): Long {
        val millisLeft = (12L * 60 * 60 * 1000) - (System.currentTimeMillis() - timestamp)
        val hours = TimeUnit.MILLISECONDS.toHours(millisLeft)
        return if (hours > 0) hours else 0
    }
}

object TrashStorage {
    val mainBinList = mutableListOf<TrashItem>()
    val stealthBinList = mutableListOf<TrashItem>()
}
