package com.smartgallery.app.ui

object FavoritesStorage {
    val favoriteList = ArrayList<MediaItem>()

    fun toggleFavorite(item: MediaItem): Boolean {
        return if (favoriteList.contains(item)) {
            favoriteList.remove(item)
            false
        } else {
            favoriteList.add(item)
            true
        }
    }

    fun isFavorite(item: MediaItem): Boolean {
        return favoriteList.contains(item)
    }
}
