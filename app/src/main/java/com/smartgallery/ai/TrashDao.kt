package com.smartgallery.ai

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_table ORDER BY deletedAt DESC")
    suspend fun getAllTrashItems(): List<TrashEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashEntity)

    @Delete
    suspend fun deleteTrashItem(item: TrashEntity)

    @Query("DELETE FROM trash_table WHERE deletedAt < :cutoffTime")
    suspend fun deleteExpiredItems(cutoffTime: Long)
}
