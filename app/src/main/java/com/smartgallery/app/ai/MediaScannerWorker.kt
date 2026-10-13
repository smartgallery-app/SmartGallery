package com.smartgallery.app.ai

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.smartgallery.app.trash.TrashManager
import java.io.File

class MediaScannerWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val classifier = NSFWClassifier(context)
    private val sampler = VideoFrameSampler(context)
    private val trashManager = TrashManager(context)

    override fun doWork(): Result {
        // Expired 24-hour files ko saaf karna
        trashManager.cleanExpiredFiles()

        val filePath = inputData.getString("FILE_PATH") ?: return Result.failure()
        val mediaFile = File(filePath)
        if (!mediaFile.exists()) return Result.failure()

        var isExplicit = false

        if (filePath.endsWith(".mp4", ignoreCase = true) || filePath.endsWith(".mkv", ignoreCase = true)) {
            // Video Sampling: 1-1 minute ke interval par scan karna
            val uri = Uri.fromFile(mediaFile)
            val frames = sampler.extractFramesAtIntervals(uri, intervalMs = 60000L)

            for (frame in frames) {
                if (classifier.isNSFW(frame)) {
                    isExplicit = true
                    break // Ek bhi explicit frame mila toh scan stop
                }
            }
        } else {
            // Photos, WhatsApp Screenshots scan
            val bitmap = BitmapFactory.decodeFile(filePath)
            if (bitmap != null && classifier.isNSFW(bitmap)) {
                isExplicit = true
            }
        }

        // Agar explicit content mila toh Vault/Trash mein move karna
        if (isExplicit) {
            val use24HourVault = inputData.getBoolean("USE_24H_VAULT", true)
            if (use24HourVault) {
                trashManager.moveTo24HourVault(mediaFile)
            } else {
                trashManager.moveToStandardTrash(mediaFile)
            }
        }

        return Result.success()
    }
}
