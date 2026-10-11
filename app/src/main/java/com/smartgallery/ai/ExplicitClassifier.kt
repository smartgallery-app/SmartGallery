package com.smartgallery.ai

object ExplicitClassifier {

    // Sirf WhatsApp media aur WhatsApp chalte waqt liye gaye Screenshots/Videos ko mark karega
    fun isExplicitContent(fileName: String, filePath: String): Boolean {
        val lowerName = fileName.lowercase()
        val lowerPath = filePath.lowercase()

        // 1. WhatsApp folder ke andar ka koi bhi photo/video
        val isWhatsAppFolder = lowerPath.contains("whatsapp")

        // 2. Clear identification for WhatsApp screenshots / recordings
        val isWhatsAppScreenshot = lowerPath.contains("whatsapp") && 
                (lowerName.contains("screenshot") || lowerName.contains("screen") || lowerName.contains("rec"))

        // Strictly target WhatsApp content & WhatsApp screenshots
        return isWhatsAppFolder || isWhatsAppScreenshot
    }
}
