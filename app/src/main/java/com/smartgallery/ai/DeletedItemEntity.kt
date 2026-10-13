package com.smartgallery.ai

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeletedItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedItem(item: DeletedItemEntity)

    @Query("DELETE FROM deleted_items WHERE uri = :uriString")
    suspend fun deleteItemByPath(uriString: String)

    @Query("SELECT * FROM deleted_items")
    suspend fun getAllDeletedItems(): List<DeletedItemEntity>
}
