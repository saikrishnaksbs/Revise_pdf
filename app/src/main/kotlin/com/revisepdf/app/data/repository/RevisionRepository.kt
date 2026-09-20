package com.revisepdf.app.data.repository

import com.revisepdf.app.data.db.AppDatabase
import com.revisepdf.app.data.db.RecallPointWithState
import com.revisepdf.app.data.db.toDomain
import com.revisepdf.app.data.db.toEntity
import com.revisepdf.core.model.ReviewOutcome
import com.revisepdf.core.model.RevisionProgress
import com.revisepdf.core.schedule.ReviewScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RevisionRepository(private val db: AppDatabase) {

    suspend fun getDueRecallPoints(
        documentId: String,
        limit: Int = 50,
        now: Long = System.currentTimeMillis(),
    ): List<RecallPointWithState> = db.revisionDao().getDue(documentId, now, limit)

    fun observeProgress(documentId: String, now: Long = System.currentTimeMillis()): Flow<RevisionProgress> = combine(
        db.revisionDao().observeTotalCount(documentId),
        db.revisionDao().observeReviewedCount(documentId),
        db.revisionDao().observeDueCount(documentId, now),
    ) { total, reviewed, due ->
        RevisionProgress(documentId = documentId, totalRecallPoints = total, reviewedAtLeastOnce = reviewed, dueNow = due)
    }

    suspend fun recordOutcome(recallPointId: String, outcome: ReviewOutcome) {
        val stateEntity = db.revisionDao().getReviewState(recallPointId) ?: return
        val next = ReviewScheduler.nextState(stateEntity.toDomain(), outcome, System.currentTimeMillis())
        db.revisionDao().updateReviewState(next.toEntity(stateEntity.documentId))
    }
}
