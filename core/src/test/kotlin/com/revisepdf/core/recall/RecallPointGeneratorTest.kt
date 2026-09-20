package com.revisepdf.core.recall

import com.revisepdf.core.model.Paragraph
import com.revisepdf.core.model.RecallType
import org.junit.Assert.assertEquals
import org.junit.Test

class RecallPointGeneratorTest {

    @Test
    fun `generates one paragraph-recall point per paragraph`() {
        val paragraph = Paragraph(
            id = "p1",
            documentId = "doc1",
            pageIndex = 4,
            orderInPage = 0,
            text = "Mitochondria are the powerhouse of the cell.",
        )
        val generator = NaiveParagraphRecallGenerator(idFactory = { "recall-1" })

        val recallPoint = generator.generate(paragraph)

        assertEquals("recall-1", recallPoint.id)
        assertEquals(paragraph.id, recallPoint.paragraphId)
        assertEquals(paragraph.documentId, recallPoint.documentId)
        assertEquals(paragraph.pageIndex, recallPoint.pageIndex)
        assertEquals(RecallType.PARAGRAPH_RECALL, recallPoint.type)
        assertEquals(paragraph.text, recallPoint.answer)
        assertEquals("Recall this paragraph from page 5.", recallPoint.prompt)
    }
}
