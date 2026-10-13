package com.smartgallery.app.ai

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NSFWClassifier(context: Context) {

    private var interpreter: Interpreter? = null

    init {
        try {
            // assets folder se tflite model load karte hain
            val modelFile = context.assets.openFd("nsfw.tflite")
            val inputStream = modelFile.createInputStream()
            val fileChannel = inputStream.channel
            val buffer = fileChannel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, modelFile.startOffset, modelFile.declaredLength)
            interpreter = Interpreter(buffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Bitmap ko scan karke dekhta hai ki NSFW score 85% se zyada hai ya nahi
     */
    fun isNSFW(bitmap: Bitmap): Boolean {
        if (interpreter == null) return false

        // Image ko 224x224 size mein scale karte hain TFLite model ke liye
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
        
        // Output array: [Safe, NSFW]
        val output = Array(1) { FloatArray(2) }
        interpreter?.run(inputBuffer, output)

        val nsfwScore = output[0][1] // Index 1 par NSFW probability hoti hai
        return nsfwScore >= 0.85f    // 85% threshold check
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(224 * 224)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }
        return byteBuffer
    }
}
