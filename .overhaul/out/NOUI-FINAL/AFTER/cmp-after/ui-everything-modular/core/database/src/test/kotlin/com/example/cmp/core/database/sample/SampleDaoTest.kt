package com.example.cmp.core.database.sample

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.example.cmp.core.database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class SampleDaoTest {

    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>(
        RuntimeEnvironment.getApplication(),
    )
        .setDriver(AndroidSQLiteDriver())
        .build()
    private val dao = database.samples()

    @After
    fun close() = database.close()

    @Test
    fun `replacing the list keeps the server's order and drops what it no longer has`() = runTest {
        dao.replaceAll(listOf(row(1, position = 0), row(2, position = 1), row(3, position = 2)))

        dao.replaceAll(listOf(row(3, position = 0), row(1, position = 1)))

        assertEquals(listOf(3, 1), dao.observeAll().first().map { it.id })
        assertNull(dao.byId(2))
    }

    @Test
    fun `an upsert writes over the row with the same id`() = runTest {
        dao.upsertAll(listOf(row(1, title = "Old")))

        dao.upsertAll(listOf(row(1, title = "New")))

        assertEquals("New", dao.byId(1)?.title)
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun `clearing the database on sign-out empties the table`() = runTest {
        dao.upsertAll(listOf(row(1), row(2)))

        database.clearAllTables()

        assertEquals(emptyList<SampleEntity>(), dao.observeAll().first())
    }

    private fun row(id: Int, title: String = "Title $id", position: Int = id) =
        SampleEntity(id = id, title = title, body = "Body", position = position)
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain.
private const val ROBOLECTRIC_SDK = 35
