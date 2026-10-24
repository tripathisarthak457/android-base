package com.example.everything.core.perfhud.frame

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameHistogramTest {

    @Test
    fun `buckets are a quarter millisecond wide up to 32 ms and coarser past it`() {
        assertEquals(0, FrameHistogram.bucketOf(0.1f))
        assertEquals(33, FrameHistogram.bucketOf(8.33f))
        assertEquals(8.25f, FrameHistogram.lowerEdge(33), 0f)
        assertEquals(8.5f, FrameHistogram.upperEdge(33), 0f)
        assertEquals(32f, FrameHistogram.lowerEdge(FrameHistogram.bucketOf(33f)), 0f)
        assertEquals(34f, FrameHistogram.upperEdge(FrameHistogram.bucketOf(33f)), 0f)
        assertEquals(700f, FrameHistogram.lowerEdge(FrameHistogram.bucketOf(705f)), 0f)
        assertEquals(FrameHistogram.BUCKETS - 1, FrameHistogram.bucketOf(5_000f))
        assertEquals(0, FrameHistogram.bucketOf(Float.NaN))
    }

    @Test
    fun `every bucket's upper edge is the next one's lower edge`() {
        for (bucket in 0 until FrameHistogram.BUCKETS - 1) {
            assertEquals(FrameHistogram.lowerEdge(bucket + 1), FrameHistogram.upperEdge(bucket), 0f)
            assertEquals(bucket, FrameHistogram.bucketOf(FrameHistogram.lowerEdge(bucket)))
        }
    }

    @Test
    fun `percentiles are read within a bucket's width of the exact value`() {
        val histogram = FrameHistogram()
        // 1, 2, ... 100 ms: the exact p50 is 50 ms and the p90 90 ms.
        for (millis in 1..100) histogram.add(millis.toFloat())

        assertEquals(50f, histogram.percentile(0.5f), 2f)
        assertEquals(90f, histogram.percentile(0.9f), 2f)
        assertEquals(99f, histogram.percentile(0.99f), 2f)
        assertEquals(100, histogram.total)
    }

    @Test
    fun `one repeated value is every percentile`() {
        val histogram = FrameHistogram()
        repeat(500) { histogram.add(6.1f) }

        assertEquals(6.1f, histogram.percentile(0.5f), 0.25f)
        assertEquals(6.1f, histogram.percentile(0.99f), 0.25f)
    }

    @Test
    fun `an empty histogram reads zero`() {
        assertEquals(0f, FrameHistogram().percentile(0.9f), 0f)
    }

    @Test
    fun `the difference of two copies holds the frames between them`() {
        val histogram = FrameHistogram()
        repeat(10) { histogram.add(4f) }
        val earlier = FrameHistogram().also(histogram::copyInto)
        repeat(5) { histogram.add(40f) }
        val since = FrameHistogram()

        histogram.subtract(earlier, into = since)

        assertEquals(5, since.total)
        assertEquals(40f, since.percentile(0.5f), 2f)
    }
}
