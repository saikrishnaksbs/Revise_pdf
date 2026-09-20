package com.revisepdf.core.text

object ParagraphSplitter {
    private val blankLineRegex = Regex("\n\\s*\n+")
    private val whitespaceRegex = Regex("\\s+")

    fun split(pageText: String, minWordCount: Int = 3): List<String> {
        if (pageText.isBlank()) return emptyList()
        val normalized = pageText.replace("\r\n", "\n").replace('\r', '\n')
        return normalized.split(blankLineRegex)
            .map(::joinWrappedLines)
            .map(String::trim)
            .filter { it.isNotEmpty() && wordCount(it) >= minWordCount }
    }

    private fun joinWrappedLines(candidate: String): String {
        val lines = candidate.split('\n').map(String::trim).filter(String::isNotEmpty)
        return lines.joinToString(" ").replace(whitespaceRegex, " ")
    }

    private fun wordCount(text: String): Int = text.split(whitespaceRegex).count(String::isNotBlank)
}
