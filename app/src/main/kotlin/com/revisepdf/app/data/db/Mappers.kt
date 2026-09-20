package com.revisepdf.app.data.db

import com.revisepdf.core.model.DocumentRecord
import com.revisepdf.core.model.Paragraph
import com.revisepdf.core.model.RecallPoint
import com.revisepdf.core.model.ReviewState

fun DocumentEntity.toDomain() = DocumentRecord(
    id = id,
    contentHash = contentHash,
    displayName = displayName,
    importedAtEpochMillis = importedAtEpochMillis,
    totalPages = totalPages,
    isFullyProcessed = isFullyProcessed,
)

fun Paragraph.toEntity() = ParagraphEntity(
    id = id,
    documentId = documentId,
    pageIndex = pageIndex,
    orderInPage = orderInPage,
    text = text,
)

fun RecallPoint.toEntity() = RecallPointEntity(
    id = id,
    paragraphId = paragraphId,
    documentId = documentId,
    pageIndex = pageIndex,
    type = type.name,
    prompt = prompt,
    answer = answer,
)

fun ReviewState.toEntity(documentId: String) = ReviewStateEntity(
    recallPointId = recallPointId,
    documentId = documentId,
    level = level,
    dueAtEpochMillis = dueAtEpochMillis,
    lastReviewedAtEpochMillis = lastReviewedAtEpochMillis,
    timesReviewed = timesReviewed,
)

fun ReviewStateEntity.toDomain() = ReviewState(
    recallPointId = recallPointId,
    level = level,
    dueAtEpochMillis = dueAtEpochMillis,
    lastReviewedAtEpochMillis = lastReviewedAtEpochMillis,
    timesReviewed = timesReviewed,
)
