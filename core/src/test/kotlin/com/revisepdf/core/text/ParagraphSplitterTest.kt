package com.revisepdf.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class ParagraphSplitterTest {

    @Test
    fun `splits on blank lines`() {
        val page = "First paragraph here now.\n\nSecond paragraph over there today."
        val result = ParagraphSplitter.split(page)
        assertEquals(listOf("First paragraph here now.", "Second paragraph over there today."), result)
    }

    @Test
    fun `joins hard-wrapped lines within a paragraph`() {
        val page = "This is a line\nthat was wrapped\nby the PDF renderer.\n\nA new paragraph starts here."
        val result = ParagraphSplitter.split(page)
        assertEquals(
            listOf(
                "This is a line that was wrapped by the PDF renderer.",
                "A new paragraph starts here.",
            ),
            result,
        )
    }

    @Test
    fun `filters out very short fragments like page numbers`() {
        val page = "12\n\nA proper paragraph with enough words in it."
        val result = ParagraphSplitter.split(page, minWordCount = 3)
        assertEquals(listOf("A proper paragraph with enough words in it."), result)
    }

    @Test
    fun `blank page yields no paragraphs`() {
        assertEquals(emptyList<String>(), ParagraphSplitter.split("   \n\n  "))
    }
}
