package com.smartgallery.app.ui

import android.content.Context
import android.graphics.Bitmap

object AIScannerHelper {

    fun classifyImage(context: Context, bitmap: Bitmap): String {
        // AI model classification logic
        // Categories: Screenshots, Documents, People, Nature, Food
        return "General"
    }

    fun searchMediaByQuery(query: String, allItems: List<MediaItem>): List<MediaItem> {
        return allItems.filter { item ->
            item.bucketName.contains(query, ignoreCase = true) ||
            item.uri.contains(query, ignoreCase = true)
        }
    }
}
