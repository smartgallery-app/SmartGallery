package com.smartgallery.ai

import android.content.Context
import androidx.room.*

@Dao
interface DeletedItemDao {
    @Query("SELECT * FROM deleted_items ORDER BY deletionTimestamp DESC")
    suspend fun getAllDeletedItems(): List<DeletedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: DeletedItemEntity)

    @Query("DELETE FROM deleted_items WHERE id = :itemId")
    suspend fun deleteById(itemId: Long)

    @Query("SELECT * FROM deleted_items WHERE expiryTimestamp <= :currentTime")
    suspend fun getExpiredItems(currentTime: Long): List<DeletedItemEntity>
}

@Database(entities = [DeletedItemEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deletedItemDao(): DeletedItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_gallery_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
