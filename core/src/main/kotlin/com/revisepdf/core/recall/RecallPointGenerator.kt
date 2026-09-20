package com.revisepdf.core.recall

import com.revisepdf.core.model.Paragraph
import com.revisepdf.core.model.RecallPoint
import com.revisepdf.core.model.RecallType

// Interface so V2 can add a local-Qwen-VL-backed generator (tables/diagrams/images -> real
// questions) alongside this V1 generator without touching callers or the data model.
interface RecallPointGenerator {
    fun generate(paragraph: Paragraph): RecallPoint
}

class NaiveParagraphRecallGenerator(
    private val idFactory: () -> String = { java.util.UUID.randomUUID().toString() },
) : RecallPointGenerator {
    override fun generate(paragraph: Paragraph): RecallPoint = RecallPoint(
        id = idFactory(),
        paragraphId = paragraph.id,
        documentId = paragraph.documentId,
        pageIndex = paragraph.pageIndex,
        type = RecallType.PARAGRAPH_RECALL,
        prompt = "Recall this paragraph from page ${paragraph.pageIndex + 1}.",
        answer = paragraph.text,
    )
}
