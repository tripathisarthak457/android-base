package com.example.finalafter.crui2.core.playstore.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPolicyTest {

    private val now = 1_800_000_000_000L

    @Test
    fun `nobody is asked before the fifth launch`() {
        assertFalse(shouldAskForReview(launches = LAUNCHES_BEFORE_ASKING - 1, lastAskedMillis = null, nowMillis = now))
        assertTrue(shouldAskForReview(launches = LAUNCHES_BEFORE_ASKING, lastAskedMillis = null, nowMillis = now))
    }

    @Test
    fun `someone asked recently is not asked again until the interval has passed`() {
        val interval = TIME_BETWEEN_ASKING.inWholeMilliseconds

        assertFalse(shouldAskForReview(launches = 40, lastAskedMillis = now - interval + 1, nowMillis = now))
        assertTrue(shouldAskForReview(launches = 40, lastAskedMillis = now - interval, nowMillis = now))
    }
}
