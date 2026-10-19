// <opt:!database>
package com.base.app.data.sample

import com.base.app.core.common.result.AppError
import com.base.app.data.sample.remote.SampleApi
import com.base.app.data.sample.remote.dto.SampleDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkSampleRepositoryTest {

    private val client = FakeSampleClient(listOf(SampleDto(1, "Alpha"), SampleDto(2, "Beta")))
    private val repository = NetworkSampleRepository(SampleApi(client))

    @Test
    fun `the list is empty until a refresh, then it is what came back`() = runTest {
        assertEquals(emptyList<Int>(), repository.items.first().map { it.id })

        repository.refresh()

        assertEquals(listOf(1, 2), repository.items.first().map { it.id })
        assertEquals("Beta", repository.peek(2)?.title)
    }

    @Test
    fun `a failed refresh keeps the last list`() = runTest {
        repository.refresh()
        client.failure = AppError.Timeout

        repository.refresh()

        assertEquals(2, repository.items.first().size)
        assertNull(repository.peek(3))
    }
}
// </opt:!database>
