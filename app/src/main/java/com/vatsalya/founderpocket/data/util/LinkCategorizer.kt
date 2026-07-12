package com.vatsalya.founderpocket.data.util

object LinkCategorizer {
    fun categorize(url: String): String = when {
        Regex("""github\.com""").containsMatchIn(url)            -> "repo"
        Regex("""arxiv\.org|\w+\.edu""").containsMatchIn(url)   -> "paper"
        Regex("""linkedin\.com""").containsMatchIn(url)          -> "post"
        Regex("""youtube\.com|youtu\.be""").containsMatchIn(url) -> "video"
        else                                                      -> "web"
    }

    val displayLabels = mapOf(
        "repo"  to "Repo",
        "paper" to "Paper",
        "post"  to "Post",
        "video" to "Video",
        "web"   to "Web"
    )
}
