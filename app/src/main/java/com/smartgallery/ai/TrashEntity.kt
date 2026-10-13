package com.smartgallery.ai

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_items")
data class TrashEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val deletionTimestamp: Long,
    val expiryTimestamp: Long,
    val isAiFlagged: Boolean
)
