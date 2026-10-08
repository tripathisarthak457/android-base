package com.base.app.core.network

import com.base.app.core.common.AppResult
import com.base.app.core.network.model.HttpMethodType
import com.base.app.core.network.model.NetworkRequest
import com.base.app.core.network.model.NetworkResponse
import com.base.app.core.testing.FakeNetworkMonitor
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class QueuedRequestReplayerTest {

    private val queue = RoomRequestQueue(FakeQueueDao())
    private val client = ScriptedClient()
    private val monitor = FakeNetworkMonitor(initiallyOnline = false)
    private val replayer = QueuedRequestReplayer(queue, client, monitor)

    @Test
    fun `sends everything in order and empties the queue`() = runTest {
        queue.enqueue(post("/a"))
        queue.enqueue(post("/b"))

        replayer.replayPending()

        assertEquals(listOf("/a", "/b"), client.sent.map { it.path })
        assertEquals(emptyList<QueuedRequest>(), queue.pending())
    }

    @Test
    fun `a request the server refuses is dropped and the pass continues`() = runTest {
        queue.enqueue(post("/refused"))
        queue.enqueue(post("/b"))
        client.codes["/refused"] = 422

        replayer.replayPending()

        assertEquals(listOf("/refused", "/b"), client.sent.map { it.path })
        assertEquals(emptyList<QueuedRequest>(), queue.pending())
    }

    @Test
    fun `a server error stops the pass and keeps the rest for next time`() = runTest {
        queue.enqueue(post("/down"))
        queue.enqueue(post("/b"))
        client.codes["/down"] = 503

        replayer.replayPending()

        assertEquals(listOf("/down"), client.sent.map { it.path })
        assertEquals(listOf("/down", "/b"), queue.pending().map { it.path })
    }

    @Test
    fun `the body, headers and query survive the round trip through the queue`() = runTest {
        val body = buildJsonObject { put("text", JsonPrimitive("hello")) }
        queue.enqueue(
            NetworkRequest(
                method = HttpMethodType.PUT,
                path = "/notes/1",
                query = mapOf("draft" to true, "tag" to "a b"),
                headers = mapOf("X-Client" to "test"),
                body = body,
            ),
        )

        replayer.replayPending()

        val sent = client.sent.single()
        assertEquals(HttpMethodType.PUT, sent.method)
        assertEquals("/notes/1?draft=true&tag=a%20b", sent.path)
        assertEquals(mapOf("X-Client" to "test"), sent.headers)
        assertEquals(body, sent.body)
    }

    @Test
    fun `replays when the device comes back online`() = runTest {
        queue.enqueue(post("/a"))
        val job = launch { replayer.replayWhenOnline() }
        advanceUntilIdle()
        assertEquals(emptyList<NetworkRequest>(), client.sent)

        monitor.setOnline(true)
        advanceUntilIdle()

        assertEquals(listOf("/a"), client.sent.map { it.path })
        job.cancel()
    }

    private fun post(path: String) = NetworkRequest(method = HttpMethodType.POST, path = path)

    private class ScriptedClient : NetworkClient {
        val sent = mutableListOf<NetworkRequest>()
        val codes = mutableMapOf<String, Int>()

        override suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse> {
            sent += request
            val code = codes[request.path] ?: 200
            return if (code < 300) {
                AppResult.Success(NetworkResponse(statusCode = code, body = "{}", headers = emptyMap()))
            } else {
                AppResult.Failure(code = code)
            }
        }
    }

    private class FakeQueueDao : QueuedRequestDao {
        private val rows = mutableListOf<QueuedRequestEntity>()

        override suspend fun all() = rows.toList()

        override suspend fun insert(entity: QueuedRequestEntity) {
            rows += entity.copy(id = rows.size + 1L)
        }

        override suspend fun delete(id: Long) {
            rows.removeAll { it.id == id }
        }

        override suspend fun clear() = rows.clear()
    }
}
