package com.revisepdf.core.ai

object QuestionPrompt {

    const val SYSTEM = "You write active-recall study questions. " +
        "Given study material, you write ONE question that tests whether the reader remembers " +
        "the key fact in it, and the short answer to that question. " +
        "Answer only with the two lines:\nQ: <question>\nA: <answer>\n" +
        "Never add commentary, explanation, or extra lines."

    fun forParagraph(paragraphText: String): String =
        "Write one active-recall question and its short answer for this passage.\n\n$paragraphText"

    fun forPageImage(pageNumber: Int): String =
        "This is page $pageNumber of a document. Write one active-recall question and its short " +
            "answer about the most important fact, figure, table, or diagram shown on it."
}
