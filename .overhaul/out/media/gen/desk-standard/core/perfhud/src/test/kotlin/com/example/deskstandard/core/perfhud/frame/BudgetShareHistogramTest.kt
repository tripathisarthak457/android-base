package com.example.deskstandard.core.perfhud.frame

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetShareHistogramTest {

    @Test
    fun `shares past four intervals share the last bucket`() {
        assertEquals(0, BudgetShareHistogram.bucketOf(0.01f))
        assertEquals(50, BudgetShareHistogram.bucketOf(1f))
        assertEquals(BudgetShareHistogram.BUCKETS - 2, BudgetShareHistogram.bucketOf(3.999f))
        assertEquals(BudgetShareHistogram.BUCKETS - 1, BudgetShareHistogram.bucketOf(4f))
        assertEquals(BudgetShareHistogram.BUCKETS - 1, BudgetShareHistogram.bucketOf(Float.POSITIVE_INFINITY))
        assertEquals(0, BudgetShareHistogram.bucketOf(Float.NaN))
    }

    @Test
    fun `percentiles are read within a bucket's width of the exact value`() {
        val histogram = BudgetShareHistogram()
        // 0.01, 0.02, ... 1.00 of the interval: the exact p90 is 0.9.
        for (hundredths in 1..100) histogram.add(hundredths / 100f)

        assertEquals(0.5f, histogram.percentile(0.5f), BudgetShareHistogram.STEP)
        assertEquals(0.9f, histogram.percentile(0.9f), BudgetShareHistogram.STEP)
        assertEquals(100, histogram.total)
    }

    @Test
    fun `a share past the last bucket reads as four intervals`() {
        val histogram = BudgetShareHistogram()
        repeat(10) { histogram.add(9f) }

        assertEquals(BudgetShareHistogram.END, histogram.percentile(0.99f), 0f)
    }

    @Test
    fun `clear empties it`() {
        val histogram = BudgetShareHistogram()
        repeat(10) { histogram.add(0.5f) }

        histogram.clear()

        assertEquals(0, histogram.total)
        assertEquals(0f, histogram.percentile(0.9f), 0f)
    }
}
