package com.base.app.core.perfhud.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemoryLogTest {

    private val log = MemoryLog(capacity = 4)

    @Test
    fun `garbage collections are counted from the first sample`() {
        log.add(reading(heap = 10f, gc = GcCounters(count = 40, millis = 400, blockingCount = 2, blockingMillis = 30)))
        log.add(reading(heap = 30f, gc = GcCounters(count = 43, millis = 420, blockingCount = 3, blockingMillis = 45)))

        val summary = checkNotNull(log.summary())

        assertEquals(3L, summary.gcCount)
        assertEquals(20L, summary.gcMillis)
        assertEquals(1L, summary.blockingGcCount)
        assertEquals(15L, summary.blockingGcMillis)
        assertEquals(30f, summary.javaHeapMb, 0f)
        assertEquals(30f, summary.peakJavaHeapMb, 0f)
    }

    @Test
    fun `collections between two runs of the sampler are not counted`() {
        log.add(reading(heap = 10f, gc = GcCounters(count = 40, millis = 400, blockingCount = 2, blockingMillis = 30)))
        log.add(reading(heap = 10f, gc = GcCounters(count = 42, millis = 410, blockingCount = 2, blockingMillis = 30)))
        // Recording off, or the app in the background: the collector went on without the sampler.
        log.startRun()
        log.add(reading(heap = 10f, gc = GcCounters(count = 90, millis = 900, blockingCount = 9, blockingMillis = 99)))
        log.add(reading(heap = 10f, gc = GcCounters(count = 91, millis = 905, blockingCount = 9, blockingMillis = 99)))

        val summary = checkNotNull(log.summary())

        assertEquals(3L, summary.gcCount)
        assertEquals(15L, summary.gcMillis)
        assertEquals(0L, summary.blockingGcCount)
    }

    @Test
    fun `PSS stands at its last measurement until it is measured again`() {
        log.add(reading(pss = 100f, graphics = 55f))
        log.add(reading())

        val summary = checkNotNull(log.summary())

        assertEquals(100f, summary.pssMb!!, 0f)
        assertEquals(45f, summary.appPssMb!!, 0f)
    }

    @Test
    fun `PSS never measured reads as unknown`() {
        log.add(reading())

        assertNull(checkNotNull(log.summary()).pssMb)
        assertNull(log.summary()?.appPssMb)
    }

    @Test
    fun `the graph gets the newest samples, oldest first`() {
        repeat(6) { log.add(reading(heap = it.toFloat())) }
        val series = MemorySeries(capacity = 3)

        log.copyRecent(series)

        assertEquals(3, series.count)
        assertEquals(listOf(3f, 4f, 5f), series.javaHeapMb.toList())
    }

    @Test
    fun `nothing sampled is no summary`() {
        assertNull(log.summary())
    }

    private fun reading(
        heap: Float = 20f,
        pss: Float = Float.NaN,
        graphics: Float = Float.NaN,
        gc: GcCounters = GcCounters(0, 0, 0, 0),
    ) = MemoryReading(javaHeapMb = heap, nativeHeapMb = 10f, pssMb = pss, graphicsMb = graphics, gc = gc)
}
