package com.smartgallery.ai

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class ExpiryCleanupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.deletedItemDao()
        val currentTime = System.currentTimeMillis()

        val expiredItems = dao.getExpiredItems(currentTime)
        for (item in expiredItems) {
            try {
                val file = File(item.originalUri)
                if (file.exists()) {
                    file.delete()
                }
                dao.deleteById(item.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success()
    }
    }
    
