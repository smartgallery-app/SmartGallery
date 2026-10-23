package com.smartgallery.app.ui

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class MediaBackupScanWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 24/7 Sato din background continuous audit aur double-check scan
            AIScannerHelper.scanAndFilterMedia(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        fun scheduleBackupEngine(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val backupRequest = PeriodicWorkRequestBuilder<MediaBackupScanWorker>(
                15, TimeUnit.MINUTES // Har 15 minute mein automatic background backup scan
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SmartGalleryBackupEngine",
                ExistingPeriodicWorkPolicy.KEEP,
                backupRequest
            )
        }
    }
}
