package com.smartgallery.app.ui

data class TrashItem(
    val id: Long,
    val uri: String,
    val deletedTime: Long,
    val isStealth: Boolean = false
)

object TrashStorage {
    val trashBinList = mutableListOf<TrashItem>()
    val stealthBinList = mutableListOf<TrashItem>()
}
