package com.smartgallery.app.ai

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File

class MediaScannerWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val classifier = NSFWClassifier(context)
    private val sampler = VideoFrameSampler(context)

    override fun doWork(): Result {
        val filePath = workerParams.inputData.getString("FILE_PATH") ?: return Result.failure()
        val mediaFile = File(filePath)
        if (!mediaFile.exists()) return Result.failure()

        var isExplicit = false

        try {
            // 1. Video Frame Scan (Video ke andar ke frames extract karke)
            if (filePath.endsWith(".mp4", ignoreCase = true) || filePath.endsWith(".mkv", ignoreCase = true)) {
                val uri = Uri.fromFile(mediaFile)
                val frames = sampler.extractFramesAtIntervals(uri, 60000L)
                for (frame in frames) {
                    if (classifier.isNSFW(frame)) {
                        isExplicit = true
                        break
                    }
                }
            } 
            // 2. Photo Pixel Scan (Photo ke pixels decode karke)
            else {
                val bitmap = BitmapFactory.decodeFile(filePath)
                if (bitmap != null && classifier.isNSFW(bitmap)) {
                    isExplicit = true
                }
            }

            // 3. Agar explicit/ashlil content mila -> Direct Permanent AUTO-DELETE
            if (isExplicit) {
                if (mediaFile.exists()) {
                    mediaFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }

        return Result.success()
    }
}
