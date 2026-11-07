private fun loadMediaFiles() {
    try {
        mediaList.clear()
        albumList.clear()
        val albumMap = mutableMapOf<String, MutableList<MediaItem>>()

        // 1. Load Images safely
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        contentResolver.query(
            imageUri,
            imageProjection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val dateAdded = cursor.getLong(dateCol)
                val bucket = cursor.getString(bucketCol) ?: "Camera"

                val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()).toString()

                val item = MediaItem(id, contentUri, dateAdded, bucket)
                mediaList.add(item)

                if (!albumMap.containsKey(bucket)) {
                    albumMap[bucket] = mutableListOf()
                }
                albumMap[bucket]?.add(item)
            }
        }

        // 2. Load Videos safely
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        contentResolver.query(
            videoUri,
            videoProjection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val dateAdded = cursor.getLong(dateCol)
                val bucket = cursor.getString(bucketCol) ?: "Camera"

                val contentUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString()).toString()

                val item = MediaItem(id, contentUri, dateAdded, bucket)
                mediaList.add(item)

                if (!albumMap.containsKey(bucket)) {
                    albumMap[bucket] = mutableListOf()
                }
                albumMap[bucket]?.add(item)
            }
        }

        // Sort all media by date added
        mediaList.sortByDescending { it.dateAdded }

        for ((name, items) in albumMap) {
            if (items.isNotEmpty()) {
                albumList.add(AlbumItem(name, items.size, items[0].uri))
            }
        }

        mainAdapter.notifyDataSetChanged()
        albumsAdapter.notifyDataSetChanged()
        Log.d(TAG, "Successfully loaded ${mediaList.size} media items.")
    } catch (e: Exception) {
        Log.e(TAG, "Error loading media files: ${e.message}", e)
    }
}
