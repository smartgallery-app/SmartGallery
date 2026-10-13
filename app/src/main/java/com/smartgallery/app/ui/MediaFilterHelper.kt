package com.smartgallery.app.ui

import android.net.Uri
import java.io.File

object MediaFilterHelper {

    enum class MediaCategory {
        ALL,
        SCREENSHOTS,
        VIDEOS,
        VAULT_24H,
        STANDARD_TRASH
    }

    data class MediaItem(
        val file: File,
        val uri: Uri,
        val category: MediaCategory,
        val timestamp: Long
    )

    /**
     * Files ko user ke select kiye hue filter/category ke mutabiq filter karega
     */
    fun filterMedia(items: List<MediaItem>, category: MediaCategory): List<MediaItem> {
        return when (category) {
            MediaCategory.ALL -> items.filter { it.category != MediaCategory.VAULT_24H && it.category != MediaCategory.STANDARD_TRASH }
            MediaCategory.SCREENSHOTS -> items.filter { it.category == MediaCategory.SCREENSHOTS }
            MediaCategory.VIDEOS -> items.filter { it.category == MediaCategory.VIDEOS }
            MediaCategory.VAULT_24H -> items.filter { it.category == MediaCategory.VAULT_24H }
            MediaCategory.STANDARD_TRASH -> items.filter { it.category == MediaCategory.STANDARD_TRASH }
        }
    }
}
