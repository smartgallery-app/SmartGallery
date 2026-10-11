package com.smartgallery.ai

object ExplicitClassifier {

    // Local heuristic-based analysis for privacy & sensitive content identification
    fun isExplicitContent(fileName: String, filePath: String): Boolean {
        val lowerName = fileName.lowercase()
        val lowerPath = filePath.lowercase()

        // Keywords or pattern checks for local privacy classification
        val sensitiveKeywords = listOf("private", "hidden", "secure", "confidential", "adult")
        
        return sensitiveKeywords.any { lowerName.contains(it) || lowerPath.contains(it) }
    }
}
