package com.revisepdf.core.model

data class DocumentRecord(
    val id: String,
    val contentHash: String,
    val displayName: String,
    val importedAtEpochMillis: Long,
    val totalPages: Int,
    val isFullyProcessed: Boolean,
)

data class Paragraph(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val orderInPage: Int,
    val text: String,
)

enum class RecallType { PARAGRAPH_RECALL, CLOZE, QUESTION }

data class RecallPoint(
    val id: String,
    val paragraphId: String,
    val documentId: String,
    val pageIndex: Int,
    val type: RecallType,
    val prompt: String,
    val answer: String,
)

data class ReviewState(
    val recallPointId: String,
    val level: Int,
    val dueAtEpochMillis: Long,
    val lastReviewedAtEpochMillis: Long?,
    val timesReviewed: Int,
)

enum class ReviewOutcome { REMEMBERED, FORGOT }

enum class SessionState { STOPPED, RUNNING, PAUSED }

data class RevisionProgress(
    val documentId: String?,
    val totalRecallPoints: Int,
    val reviewedAtLeastOnce: Int,
    val dueNow: Int,
)
