package com.base.app.core.database.queue

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class QueuedRequestDaoTest {

    private val database = Room.inMemoryDatabaseBuilder<QueueDatabase>(RuntimeEnvironment.getApplication())
        .setDriver(AndroidSQLiteDriver())
        .build()
    private val dao = database.queuedRequests()

    @After
    fun close() = database.close()

    @Test
    fun `only the owner's entries and public ones may be sent, oldest first`() = runTest {
        dao.insert(entry(owner = "ada", path = "/a"))
        dao.insert(entry(owner = "grace", path = "/b"))
        dao.insert(entry(owner = null, path = "/c"))

        assertEquals(listOf("/a", "/c"), dao.sendable("ada").map { it.path })
        assertEquals(listOf("/c"), dao.sendable(null).map { it.path })
    }

    @Test
    fun `a refused entry waits until it is put back in line`() = runTest {
        val id = dao.insert(entry(owner = "ada", path = "/a"))
        dao.countAttempt(id)

        dao.markRefused(id, at = 1, status = 422, code = "invalid", message = "No")
        val refused = dao.sendable("ada")
        dao.requeue(id)

        assertEquals(emptyList<QueuedRequestEntity>(), refused)
        assertEquals(0, dao.sendable("ada").single().attempts)
    }

    private fun entry(owner: String?, path: String) = QueuedRequestEntity(
        owner = owner,
        method = "POST",
        path = path,
        headersJson = "{}",
        body = null,
        createdAtEpochMillis = 0,
    )
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain.
private const val ROBOLECTRIC_SDK = 35
