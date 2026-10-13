package com.smartgallery.ai

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_items")
data class DeletedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val deletedAt: Long,
    val expiresAt: Long,
    val isAiFlagged: Boolean
)
