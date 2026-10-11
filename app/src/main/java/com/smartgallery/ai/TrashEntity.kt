package com.smartgallery.ai

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_table")
data class TrashEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val deletedAt: Long,
    val isExplicit: Boolean // true for 24-hour explicit trash, false for 7-day normal trash
)
