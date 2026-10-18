package com.example.hud.everything.core.network.queue

import com.example.hud.everything.core.common.result.AppError
import com.example.hud.everything.core.common.result.AppResult
import com.example.hud.everything.core.common.time.AppClock
import com.example.hud.everything.core.database.queue.QueuedRequestDao
import com.example.hud.everything.core.database.queue.QueuedRequestEntity
import com.example.hud.everything.core.network.MemoryTokenStore
import com.example.hud.everything.core.network.client.NetworkClient
import com.example.hud.everything.core.network.model.NetworkRequest
import com.example.hud.everything.core.network.model.NetworkResponse
import com.example.hud.everything.core.network.model.RequestBody
import com.example.hud.everything.core.network.tokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class QueuedRequestReplayerTest {

    private var now = Instant.fromEpochSeconds(1_000_000)
    private val dao = FakeQueueDao()
    private val store = MemoryTokenStore(tokens(owner = "ada"))
    private val client = ScriptedClient()
    private val replayer = QueuedRequestReplayer(dao, client, store, AppClock { now })

    @Test
    fun `sends everything in order and empties the queue`() = runTest {
        dao.insert(entry("/a"))
        dao.insert(entry("/b"))

        val left = replayer.replayPending()

        assertEquals(0, left)
        assertEquals(listOf("/a", "/b"), client.sent.map { it.path })
        assertEquals(emptyList<QueuedRequestEntity>(), dao.rows.value)
    }

    @Test
    fun `a request the server refuses is kept with its answer and the pass goes on`() = runTest {
        dao.insert(entry("/refused"))
        dao.insert(entry("/b"))
        client.failures["/refused"] = AppError.Http(status = 422, code = "invalid", message = "Odometer went back")

        replayer.replayPending()

        assertEquals(listOf("/refused", "/b"), client.sent.map { it.path })
        val kept = dao.rows.value.single()
        assertEquals(422, kept.refusedStatus)
        assertEquals("invalid", kept.refusedCode)
        assertEquals("Odometer went back", kept.refusedMessage)
    }

    @Test
    fun `no answer or a server error ends the pass and keeps the order`() = runTest {
        dao.insert(entry("/down"))
        dao.insert(entry("/b"))
        client.failures["/down"] = AppError.Http(status = 503)

        val left = replayer.replayPending()

        assertEquals(2, left)
        assertEquals(listOf("/down"), client.sent.map { it.path })
        assertEquals(1, dao.rows.value.first().attempts)
    }

    @Test
    fun `answers about the moment rather than the request keep it waiting`() = runTest {
        dao.insert(entry("/a"))

        listOf(401, 408, 426, 429).forEach { status ->
            client.failures["/a"] = AppError.Http(status = status)
            replayer.replayPending()
        }
        client.failures["/a"] = AppError.Timeout
        replayer.replayPending()

        val waiting = dao.rows.value.single()
        assertNull(waiting.refusedAtEpochMillis)
        assertEquals(5, waiting.attempts)
    }

    @Test
    fun `an entry is sent only with its owner's session`() = runTest {
        dao.insert(entry("/ada", owner = "ada"))
        dao.insert(entry("/grace", owner = "grace"))
        dao.insert(entry("/public", owner = null))

        replayer.replayPending()

        assertEquals(listOf("/ada", "/public"), client.sent.map { it.path })
        assertEquals(listOf(true, false), client.sent.map { it.requiresAuth })
        assertEquals("/grace", dao.rows.value.single().path)
    }

    @Test
    fun `a pass stops when someone else signs in during it`() = runTest {
        dao.insert(entry("/a"))
        dao.insert(entry("/b"))
        client.onSend = { if (it.path == "/a") store.save(tokens(access = "B1", owner = "grace")) }

        val left = replayer.replayPending()

        assertEquals(0, left)
        assertEquals(listOf("/a"), client.sent.map { it.path })
        assertEquals(0, dao.rows.value.single().attempts)
    }

    @Test
    fun `signed out, only public entries are sent`() = runTest {
        store.clear()
        dao.insert(entry("/ada", owner = "ada"))

        val left = replayer.replayPending()

        assertEquals(0, left)
        assertEquals(emptyList<NetworkRequest>(), client.sent)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun `an entry too old to matter is given up on, and kept`() = runTest {
        dao.insert(entry("/stale"))
        now += 8.days

        replayer.replayPending()

        assertEquals(emptyList<NetworkRequest>(), client.sent)
        assertEquals(now.toEpochMilliseconds(), dao.rows.value.single().refusedAtEpochMillis)
    }

    @Test
    fun `the body, headers and query survive the round trip`() = runTest {
        dao.insert(entry("/notes/1?draft=true", headersJson = """{"Idempotency-Key":"k1"}""", body = """{"a":1}"""))

        replayer.replayPending()

        val sent = client.sent.single()
        assertEquals("/notes/1?draft=true", sent.path)
        assertEquals(mapOf("Idempotency-Key" to "k1"), sent.headers)
        assertEquals(RequestBody.Json("""{"a":1}"""), sent.body)
    }

    private fun entry(
        path: String,
        owner: String? = "ada",
        headersJson: String = "{}",
        body: String? = null,
    ) = QueuedRequestEntity(
        owner = owner,
        method = "POST",
        path = path,
        headersJson = headersJson,
        body = body,
        createdAtEpochMillis = now.toEpochMilliseconds(),
    )

    private class ScriptedClient : NetworkClient {
        val sent = mutableListOf<NetworkRequest>()
        val failures = mutableMapOf<String, AppError>()
        var onSend: suspend (NetworkRequest) -> Unit = {}

        override suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse> {
            sent += request
            onSend(request)
            return failures[request.path]?.let { AppResult.Failure(it) } ?: AppResult.Success(NetworkResponse(200, "{}"))
        }

        override suspend fun <T> execute(request: NetworkRequest, deserializer: DeserializationStrategy<T>): AppResult<T> =
            error("The replayer sends raw requests.")
    }

    private class FakeQueueDao : QueuedRequestDao {
        val rows = MutableStateFlow<List<QueuedRequestEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insert(entity: QueuedRequestEntity): Long {
            val id = nextId++
            rows.value += entity.copy(id = id)
            return id
        }

        override suspend fun sendable(owner: String?): List<QueuedRequestEntity> =
            rows.value.filter { it.refusedAtEpochMillis == null && (it.owner == null || it.owner == owner) }

        override fun observe(owner: String?): Flow<List<QueuedRequestEntity>> =
            rows.map { all -> all.filter { it.owner == null || it.owner == owner } }

        override suspend fun countAttempt(id: Long) = update(id) { it.copy(attempts = it.attempts + 1) }

        override suspend fun markRefused(id: Long, at: Long, status: Int?, code: String?, message: String?) =
            update(id) { it.copy(refusedAtEpochMillis = at, refusedStatus = status, refusedCode = code, refusedMessage = message) }

        override suspend fun requeue(id: Long) =
            update(id) { it.copy(refusedAtEpochMillis = null, refusedStatus = null, refusedCode = null, refusedMessage = null, attempts = 0) }

        override suspend fun delete(id: Long) {
            rows.value = rows.value.filterNot { it.id == id }
        }

        private fun update(id: Long, change: (QueuedRequestEntity) -> QueuedRequestEntity) {
            rows.value = rows.value.map { if (it.id == id) change(it) else it }
        }
    }
}
