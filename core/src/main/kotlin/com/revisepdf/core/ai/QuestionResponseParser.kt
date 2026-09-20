package com.revisepdf.core.ai

data class GeneratedQuestion(val question: String, val answer: String)

// Small on-device models drift from the requested "Q:/A:" format constantly — they wrap lines,
// add markdown, rename the labels, or collapse both onto one line. Returning null lets the caller
// keep the plain paragraph recall point rather than storing garbage.
object QuestionResponseParser {
    private val fenceRegex = Regex("```[a-zA-Z]*")
    private val emphasisRegex = Regex("\\*\\*|__")
    private val questionRegex = Regex("^\\s*(?:q|question)\\s*[:.)\\-]\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val answerRegex = Regex("^\\s*(?:a|answer)\\s*[:.)\\-]\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val inlineAnswerRegex = Regex("\\s(?:a|answer)\\s*[:.)\\-]\\s*(.*)$", RegexOption.IGNORE_CASE)

    fun parse(raw: String): GeneratedQuestion? {
        if (raw.isBlank()) return null
        val lines = raw.replace(fenceRegex, "").replace(emphasisRegex, "").lines()

        var questionIndex = -1
        var question = ""
        for ((index, line) in lines.withIndex()) {
            val match = questionRegex.find(line) ?: continue
            question = match.groupValues[1].trim()
            questionIndex = index
            break
        }
        if (questionIndex < 0) return null

        inlineAnswerRegex.find(question)?.let { inline ->
            val inlineAnswer = inline.groupValues[1].trim()
            val questionOnly = question.substring(0, inline.range.first).trim()
            if (questionOnly.isNotEmpty() && inlineAnswer.isNotEmpty()) {
                return GeneratedQuestion(questionOnly, inlineAnswer)
            }
        }

        var answerIndex = -1
        val answer = StringBuilder()
        for (index in (questionIndex + 1) until lines.size) {
            val match = answerRegex.find(lines[index]) ?: continue
            answer.append(match.groupValues[1].trim())
            answerIndex = index
            break
        }
        if (answerIndex < 0) return null

        if (question.isEmpty()) {
            for (index in (questionIndex + 1) until answerIndex) {
                if (lines[index].isBlank()) continue
                question = lines[index].trim()
                break
            }
        }

        for (index in (answerIndex + 1) until lines.size) {
            val line = lines[index]
            if (line.isBlank()) break
            if (questionRegex.containsMatchIn(line) || answerRegex.containsMatchIn(line)) break
            answer.append(' ').append(line.trim())
        }

        val finalAnswer = answer.toString().trim()
        if (question.isEmpty() || finalAnswer.isEmpty()) return null
        return GeneratedQuestion(question = question, answer = finalAnswer)
    }
}
