package com.smartgallery.app.trash

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

class TrashManager(private val context: Context) {

    // Recovery folders ke path define karte hain
    private val vault24HoursDir = File(context.filesDir, "Vault_24Hours")
    private val standardTrashDir = File(context.filesDir, "Standard_Trash")

    init {
        if (!vault24HoursDir.exists()) vault24HoursDir.mkdirs()
        if (!standardTrashDir.exists()) standardTrashDir.mkdirs()
    }

    /**
     * Detect ki gayi explicit media ko 24-ghante ke temporary vault mein move karna
     */
    fun moveTo24HourVault(sourceFile: File): Boolean {
        return try {
            val destination = File(vault24HoursDir, sourceFile.name)
            sourceFile.renameTo(destination)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Standard 30-day trash mein move karna (Normal gallery style)
     */
    fun moveToStandardTrash(sourceFile: File): Boolean {
        return try {
            val destination = File(standardTrashDir, sourceFile.name)
            sourceFile.renameTo(destination)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 24 ghante purane files ko permanently delete karne ka background check
     */
    fun cleanExpiredFiles() {
        val currentTime = System.currentTimeMillis()
        val expiryLimit = TimeUnit.HOURS.toMillis(24)

        vault24HoursDir.listFiles()?.forEach { file ->
            if (currentTime - file.lastModified() > expiryLimit) {
                file.delete() // 24 ghante baad permanent delete
            }
        }
    }
}
