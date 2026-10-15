package com.smartgallery.app.ai

import android.content.Context

class AppDatabase(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = nil

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
