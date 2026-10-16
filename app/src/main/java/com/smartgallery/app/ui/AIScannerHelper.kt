package com.smartgallery.app.ui

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

object AIScannerHelper {

    // Sensitive/explicit keywords to detect
    private val sensitiveKeywords = listOf("adult", "nudity", "explicit", "undergarment", "erotic", "nsfw")

    fun scanImageForSafetyAndTags(
        context: Context,
        imageUri: Uri,
        onResult: (isSensitive: Boolean, detectedTags: List<String>) -> Unit
    ) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

            labeler.process(inputImage)
                .addOnSuccessListener { labels ->
                    val detectedTags = mutableListOf<String>()
                    var isSensitive = false

                    for (label in labels) {
                        val text = label.text.lowercase()
                        val confidence = label.confidence

                        detectedTags.add(text)

                        if (confidence > 0.6f && sensitiveKeywords.any { text.contains(it) }) {
                            isSensitive = true
                        }
                    }

                    onResult(isSensitive, detectedTags)
                }
                .addOnFailureListener {
                    onResult(false, emptyList())
                }
        } catch (e: Exception) {
            onResult(false, emptyList())
        }
    }
}
