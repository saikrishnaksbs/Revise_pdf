package com.revisepdf.core.schedule

import com.revisepdf.core.model.ReviewOutcome
import com.revisepdf.core.model.ReviewState

object ReviewScheduler {
    private val levelIntervalsMinutes = listOf(0L, 10L, 60L, 360L, 1440L, 4320L, 10080L, 20160L)
    val maxLevel: Int = levelIntervalsMinutes.lastIndex

    fun initialState(recallPointId: String, nowEpochMillis: Long): ReviewState = ReviewState(
        recallPointId = recallPointId,
        level = 0,
        dueAtEpochMillis = nowEpochMillis,
        lastReviewedAtEpochMillis = null,
        timesReviewed = 0,
    )

    fun nextState(current: ReviewState, outcome: ReviewOutcome, nowEpochMillis: Long): ReviewState {
        val newLevel = when (outcome) {
            ReviewOutcome.REMEMBERED -> (current.level + 1).coerceAtMost(maxLevel)
            ReviewOutcome.FORGOT -> 0
        }
        val dueAt = nowEpochMillis + levelIntervalsMinutes[newLevel] * 60_000L
        return current.copy(
            level = newLevel,
            dueAtEpochMillis = dueAt,
            lastReviewedAtEpochMillis = nowEpochMillis,
            timesReviewed = current.timesReviewed + 1,
        )
    }

    fun isDue(state: ReviewState, nowEpochMillis: Long): Boolean = state.dueAtEpochMillis <= nowEpochMillis
}
