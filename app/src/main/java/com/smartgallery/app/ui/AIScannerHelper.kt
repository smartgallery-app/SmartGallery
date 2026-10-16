package com.smartgallery.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

object AIScannerHelper {

    fun scanImageForSafetyAndTags(
        context: Context,
        imageUri: Uri,
        onResult: (isSensitive: Boolean, tags: List<String>) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val tagList = labels.map { it.text.lowercase() }
                    
                    // Basic sensitivity filtering keywords
                    val sensitiveKeywords = listOf("adult", "explicit", "nudity", "nsfw", "bikini", "lingerie")
                    val isSensitive = tagList.any { tag -> sensitiveKeywords.any { key -> tag.contains(key) } }

                    onResult(isSensitive, tagList)
                }
                .addOnFailureListener {
                    onResult(false, emptyList())
                }
        } catch (e: Exception) {
            onResult(false, emptyList())
        }
    }

    fun scanBitmapForSafety(
        bitmap: Bitmap,
        onResult: (isSensitive: Boolean) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val tagList = labels.map { it.text.lowercase() }
                    val sensitiveKeywords = listOf("adult", "explicit", "nudity", "nsfw", "bikini", "lingerie")
                    val isSensitive = tagList.any { tag -> sensitiveKeywords.any { key -> tag.contains(key) } }

                    onResult(isSensitive)
                }
                .addOnFailureListener {
                    onResult(false)
                }
        } catch (e: Exception) {
            onResult(false)
        }
    }
}
