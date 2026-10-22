package com.smartgallery.app.ui

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class BackgroundScanWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val currentTime = System.currentTimeMillis()
        
        // Clean 12-hour stealth bin items
        TrashStorage.stealthBinList.removeAll { item ->
            (currentTime - item.dateAdded) > (12 * 60 * 60 * 1000)
        }

        // Clean 30-day main trash items
        TrashStorage.mainBinList.removeAll { item ->
            (currentTime - item.dateAdded) > (30L * 24 * 60 * 60 * 1000)
        }

        return Result.success()
    }
}
