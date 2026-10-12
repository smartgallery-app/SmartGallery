package com.smartgallery.ai

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_items")
data class DeletedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalUri: String,
    val fileName: String,
    val mediaType: String, // "IMAGE" or "VIDEO"
    val deletionReason: String, // "AI_EXPLICIT" or "USER_DELETED"
    val deletionTimestamp: Long,
    val expiryTimestamp: Long // 24 hours for AI, 7 days for User
)
