package com.smartgallery.app

data class MediaItem(
    val id: Long,
    val uriString: String,
    val isVideo: Boolean,
    val path: String
)
