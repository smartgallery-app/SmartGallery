package com.smartgallery.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Phone start hone ke 5 minute baad WorkManager background scan chalaye-ga
            val scanRequest = OneTimeWorkRequestBuilder<BackgroundScanWorker>()
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueue(scanRequest)
        }
    }
}
