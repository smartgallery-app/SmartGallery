package com.smartgallery.app.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class RecoveryCleanupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            // Yahan 12-24 ghante purani trash items ko permanently delete karne ka logic aayega
            // Jaise ki Database ya SharedPrefs se timestamp check karke files remove karna
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun scheduleCleanup(context: Context) {
            val cleanupRequest = PeriodicWorkRequestBuilder<RecoveryCleanupWorker>(
                12, TimeUnit.HOURS // Har 12 ghante mein background cleanup run hoga
            ).build()

            WorkManager.getInstance(context).enqueue(cleanupRequest)
        }
    }
}
