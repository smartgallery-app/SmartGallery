package com.smartgallery.ai

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ExpiryCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val currentTime = System.currentTimeMillis()
            database.trashDao().deleteExpiredItems(currentTime)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
