// <opt:database>
package com.base.app.data.sample

import com.base.app.core.common.result.AppError
import com.base.app.core.common.result.AppResult
import com.base.app.core.database.sample.SampleDao
import com.base.app.core.database.sample.SampleEntity
import com.base.app.data.sample.remote.SampleApi
import com.base.app.data.sample.remote.dto.SampleDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineFirstSampleRepositoryTest {

    private val dao = FakeSampleDao()
    private val client = FakeSampleClient(listOf(SampleDto(2, "Beta"), SampleDto(1, "Alpha")))

    private fun repository() = OfflineFirstSampleRepository(SampleApi(client), dao)

    @Test
    fun `a refresh writes the server's list, in its order, and the list follows`() = runTest {
        val repository = repository()

        repository.refresh()

        assertEquals(listOf(2, 1), repository.items.first().map { it.id })
    }

    @Test
    fun `a list read as a refresh returns is the one it wrote, while a screen is already reading`() = runTest {
        val repository = repository()
        repository.items.launchIn(backgroundScope)
        runCurrent()

        repository.refresh()

        assertEquals(listOf(2, 1), repository.items.first().map { it.id })
    }

    @Test
    fun `what was stored stays on screen when a refresh fails`() = runTest {
        val repository = repository()
        repository.refresh()
        client.failure = AppError.Offline

        val result = repository.refresh()

        assertEquals(AppResult.Failure(AppError.Offline), result)
        assertEquals(listOf(2, 1), repository.items.first().map { it.id })
    }

    @Test
    fun `a write the database refuses is a storage failure`() = runTest {
        dao.refuseWrites = true

        val result = repository().refresh()

        assertEquals(true, (result as? AppResult.Failure)?.error is AppError.Storage)
    }

    @Test
    fun `peek answers from memory while the list is being read`() = runTest {
        val repository = repository()
        assertNull(repository.peek(1))

        repository.items.launchIn(backgroundScope)
        repository.refresh()
        runCurrent()

        assertEquals("Alpha", repository.peek(1)?.title)
    }

    @Test
    fun `peek forgets the list once nobody reads it`() = runTest {
        val repository = repository()
        repository.refresh()
        val reading = repository.items.launchIn(backgroundScope)
        runCurrent()

        reading.cancel()
        runCurrent()

        assertNull(repository.peek(1))
    }

    @Test
    fun `readers on many threads at once leave peek empty once the last one stops`() = runTest {
        val repository = repository()
        repository.refresh()

        withContext(Dispatchers.Default) {
            repeat(READERS) { launch { repository.items.first() } }
        }

        assertNull(repository.peek(1))
    }

    @Test
    fun `an item is read from the database before the network`() = runTest {
        val repository = repository()
        repository.refresh()
        client.paths.clear()

        val item = repository.item(1)

        assertEquals("Alpha", (item as AppResult.Success).value.title)
        assertEquals(emptyList<String>(), client.paths)
    }

    private companion object {
        /** Enough readers starting and stopping together that a count kept without care loses one. */
        const val READERS = 2_000
    }

    private class FakeSampleDao : SampleDao {
        private val rows = MutableStateFlow<List<SampleEntity>>(emptyList())
        var refuseWrites = false

        override fun observeAll(): Flow<List<SampleEntity>> = rows.map { all -> all.sortedBy { it.position } }

        override suspend fun byId(id: Int): SampleEntity? = rows.value.firstOrNull { it.id == id }

        override suspend fun upsertAll(rows: List<SampleEntity>) {
            check(!refuseWrites) { "disk full" }
            val ids = rows.map { it.id }.toSet()
            this.rows.value = this.rows.value.filterNot { it.id in ids } + rows
        }

        override suspend fun deleteAllExcept(ids: List<Int>) {
            rows.value = rows.value.filter { it.id in ids }
        }
    }
}
// </opt:database>
