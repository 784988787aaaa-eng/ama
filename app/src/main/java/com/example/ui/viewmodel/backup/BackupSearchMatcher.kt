package com.example.ui.viewmodel.backup

object BackupSearchMatcher {
    fun matchesFlexibleQuery(filename: String, query: String): Boolean {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return true

        // 1. Direct contains check
        if (filename.contains(cleanQuery, ignoreCase = true)) return true

        // 2. Token-based matching
        val queryTokens = cleanQuery.split(Regex("[^a-zA-Z0-9]")).filter { it.isNotEmpty() }
        val fileTokens = filename.split(Regex("[^a-zA-Z0-9]")).filter { it.isNotEmpty() }

        if (queryTokens.isEmpty()) return true

        // All query tokens must match at least one file token
        return queryTokens.all { qToken ->
            fileTokens.any { fToken ->
                // Match as string contains/prefix
                if (fToken.contains(qToken, ignoreCase = true)) return@any true

                // Match as numbers
                val qNum = qToken.toIntOrNull()
                val fNum = fToken.toIntOrNull()
                if (qNum != null && fNum != null && qNum == fNum) return@any true

                false
            }
        }
    }
}
