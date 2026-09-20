package com.revisepdf.core.schedule

import com.revisepdf.core.model.ReviewOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSchedulerTest {

    @Test
    fun `new recall point is due immediately`() {
        val now = 1_000_000L
        val state = ReviewScheduler.initialState("rp1", now)
        assertTrue(ReviewScheduler.isDue(state, now))
        assertEquals(0, state.level)
    }

    @Test
    fun `remembering advances the level and pushes the due date out`() {
        val now = 1_000_000L
        val state = ReviewScheduler.initialState("rp1", now)

        val next = ReviewScheduler.nextState(state, ReviewOutcome.REMEMBERED, now)

        assertEquals(1, next.level)
        assertEquals(1, next.timesReviewed)
        assertTrue(next.dueAtEpochMillis > now)
        assertFalse(ReviewScheduler.isDue(next, now))
    }

    @Test
    fun `forgetting resets the level back to zero and it is due again immediately`() {
        val now = 1_000_000L
        var state = ReviewScheduler.initialState("rp1", now)
        state = ReviewScheduler.nextState(state, ReviewOutcome.REMEMBERED, now)
        state = ReviewScheduler.nextState(state, ReviewOutcome.REMEMBERED, now)

        val next = ReviewScheduler.nextState(state, ReviewOutcome.FORGOT, now)

        assertEquals(0, next.level)
        assertTrue(ReviewScheduler.isDue(next, now))
    }

    @Test
    fun `level never exceeds the max level`() {
        val now = 1_000_000L
        var state = ReviewScheduler.initialState("rp1", now)
        repeat(ReviewScheduler.maxLevel + 5) {
            state = ReviewScheduler.nextState(state, ReviewOutcome.REMEMBERED, now)
        }
        assertEquals(ReviewScheduler.maxLevel, state.level)
    }

    private fun assertFalse(condition: Boolean) = assertTrue(!condition)
}
