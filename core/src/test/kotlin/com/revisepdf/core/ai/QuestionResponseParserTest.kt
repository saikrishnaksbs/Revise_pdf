package com.revisepdf.core.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuestionResponseParserTest {

    @Test
    fun `parses the requested two-line format`() {
        val result = QuestionResponseParser.parse("Q: What powers the cell?\nA: The mitochondria.")
        assertEquals(GeneratedQuestion("What powers the cell?", "The mitochondria."), result)
    }

    @Test
    fun `parses Question and Answer labels`() {
        val result = QuestionResponseParser.parse("Question: Who wrote Hamlet?\nAnswer: Shakespeare")
        assertEquals(GeneratedQuestion("Who wrote Hamlet?", "Shakespeare"), result)
    }

    @Test
    fun `strips markdown fences and bold markers`() {
        val raw = "```\n**Q:** What is the capital of France?\n**A:** Paris\n```"
        val result = QuestionResponseParser.parse(raw)
        assertEquals(GeneratedQuestion("What is the capital of France?", "Paris"), result)
    }

    @Test
    fun `ignores chatter before the question`() {
        val raw = "Sure! Here is your study question:\n\nQ: What year did WW2 end?\nA: 1945"
        val result = QuestionResponseParser.parse(raw)
        assertEquals(GeneratedQuestion("What year did WW2 end?", "1945"), result)
    }

    @Test
    fun `handles question and answer collapsed onto one line`() {
        val result = QuestionResponseParser.parse("Q: What is the boiling point of water? A: 100 degrees Celsius")
        assertEquals(GeneratedQuestion("What is the boiling point of water?", "100 degrees Celsius"), result)
    }

    @Test
    fun `joins a multi-line answer`() {
        val raw = "Q: Describe photosynthesis.\nA: Plants convert light into chemical energy,\nstoring it as glucose."
        val result = QuestionResponseParser.parse(raw)
        assertEquals(
            GeneratedQuestion(
                "Describe photosynthesis.",
                "Plants convert light into chemical energy, storing it as glucose.",
            ),
            result,
        )
    }

    @Test
    fun `does not mistake a sentence starting with A for an answer label`() {
        val raw = "Q: What is a cell?\nA cell is a thing\nA: The basic unit of life."
        val result = QuestionResponseParser.parse(raw)
        assertEquals(GeneratedQuestion("What is a cell?", "The basic unit of life."), result)
    }

    @Test
    fun `returns null when the answer is missing`() {
        assertNull(QuestionResponseParser.parse("Q: What is the meaning of life?"))
    }

    @Test
    fun `returns null on unstructured rambling`() {
        assertNull(QuestionResponseParser.parse("I think this passage is about biology and cells."))
    }

    @Test
    fun `returns null on blank input`() {
        assertNull(QuestionResponseParser.parse("   \n  "))
    }
}
