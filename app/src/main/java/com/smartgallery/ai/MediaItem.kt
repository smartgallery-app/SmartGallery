package com.smartgallery.ai

data class MediaItem(
    val id: Long,
    val uriString: String,
    val name: String,
    val dateAdded: Long,
    val isVideo: Boolean
)
