package com.smartgallery.ai

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedItem(item: TrashEntity)

    @Query("SELECT * FROM deleted_items")
    suspend fun getAllDeletedItems(): List<TrashEntity>

    @Query("DELETE FROM deleted_items WHERE uri = :uriString")
    suspend fun deleteItemByPath(uriString: String)

    @Query("DELETE FROM deleted_items WHERE expiryTimestamp <= :currentTime")
    suspend fun deleteExpiredItems(currentTime: Long)
}
