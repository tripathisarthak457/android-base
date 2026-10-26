package com.example.finalafter.crnoui3.core.network.websocket

import com.example.finalafter.crnoui3.core.common.coroutines.AppDispatchers
import com.example.finalafter.crnoui3.core.common.coroutines.ApplicationScope
import com.example.finalafter.crnoui3.core.common.log.Log
import com.example.finalafter.crnoui3.core.common.network.NetworkMonitor
import com.example.finalafter.crnoui3.core.common.session.SessionScopedStore
import com.example.finalafter.crnoui3.core.common.time.AppClock
import com.example.finalafter.crnoui3.core.network.auth.SessionTokens
import com.example.finalafter.crnoui3.core.network.client.NetworkConfig
import com.example.finalafter.crnoui3.core.network.di.RealtimeClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the socket is doing, for a UI that shows a "reconnecting" indicator. */
sealed interface SocketState {
    data object Disconnected : SocketState
    data object Connecting : SocketState
    data object Connected : SocketState
    data class Failed(val cause: Throwable) : SocketState
}

/**
 * One long-lived WebSocket that reconnects by itself. Nothing opens it until a feature calls
 * [connect]; from then on [RealtimeLifecycle] closes it while the app is in the background and
 * opens it again on return, and an account switch reopens it with the new session's token.
 *
 * Reconnects wait 1 s, doubling to 30 s, with jitter, and the wait resets only once the server has
 * sent a frame: a server that accepts and at once closes (an expired token) is not hammered every
 * second. While the device is offline it waits for the network instead.
 */
@Singleton
class AppWebSocket @Inject constructor(
    @RealtimeClient private val client: HttpClient,
    private val config: NetworkConfig,
    private val tokens: SessionTokens,
    private val networkMonitor: NetworkMonitor,
    private val clock: AppClock,
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
) : SessionScopedStore {

    private val _state = MutableStateFlow<SocketState>(SocketState.Disconnected)
    val state: StateFlow<SocketState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = MESSAGE_BUFFER)
    val messages: Flow<String> = _messages.asSharedFlow()

    private var wanted: Connection? = null
    private var paused = false
    private var running: Job? = null

    @Volatile
    private var session: DefaultClientWebSocketSession? = null

    /**
     * Keeps a socket open to [path] (after `NetworkConfig.webSocketUrl`) until [disconnect]. [path]
     * and [headers] are asked again on every attempt, so a reconnect can resume from a cursor.
     */
    @Synchronized
    fun connect(
        path: suspend () -> String = { "" },
        authenticated: Boolean = true,
        headers: suspend () -> Map<String, String> = { emptyMap() },
    ) {
        wanted = Connection(path, authenticated, headers)
        if (!paused) start()
    }

    @Synchronized
    fun disconnect() {
        wanted = null
        stop()
    }

    /** Sends [message] on the open socket; false when there is none. */
    suspend fun send(message: String): Boolean = withContext(dispatchers.io) {
        val open = session ?: return@withContext false
        try {
            open.send(Frame.Text(message))
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (closed: Exception) {
            Log.w("WebSocket send failed", closed, TAG)
            false
        }
    }

    /** The previous person's socket must not stay open into the next session. */
    override suspend fun clear() = disconnect()

    @Synchronized
    internal fun pause() {
        paused = true
        stop()
    }

    @Synchronized
    internal fun resume() {
        paused = false
        if (wanted != null) start()
    }

    /** Reopens with the current session's token, after the account changed under an open socket. */
    @Synchronized
    internal fun reopen() {
        if (wanted == null || paused) return
        stop()
        start()
    }

    private fun start() {
        val connection = wanted ?: return
        if (running?.isActive == true) return
        running = scope.launch(dispatchers.io) { keepOpen(connection) }
    }

    private fun stop() {
        running?.cancel()
        running = null
        // Taken now, not in the coroutine: by the time that runs, a reopen may have opened the next
        // socket, which this must neither close nor report as disconnected.
        val closing = session
        session = null
        _state.value = SocketState.Disconnected
        if (closing != null) scope.launch(dispatchers.io) { closeQuietly(closing) }
    }

    private suspend fun keepOpen(connection: Connection) {
        var attempt = 0
        while (true) {
            networkMonitor.isOnline.first { it }
            _state.value = SocketState.Connecting
            var opened: DefaultClientWebSocketSession? = null
            try {
                val token = if (connection.authenticated) freshToken() else null
                val extra = connection.headers()
                val socket = client.webSocketSession(config.resolvedWebSocketUrl + connection.path()) {
                    token?.let { headers[HttpHeaders.Authorization] = "Bearer $it" }
                    extra.forEach { (name, value) -> headers[name] = value }
                }
                opened = socket
                session = socket
                _state.value = SocketState.Connected
                for (frame in socket.incoming) {
                    attempt = 0
                    if (frame is Frame.Text) _messages.emit(frame.readText())
                }
                // The server closed it, which servers do to idle sockets routinely: reconnect.
                _state.value = SocketState.Disconnected
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                Log.w("WebSocket failed", failure, TAG)
                _state.value = SocketState.Failed(failure)
            } finally {
                if (session === opened) session = null
            }
            delay(backoffMillis(attempt++))
        }
    }

    /** The access token, renewed first when it is known to have expired, since the server would refuse the handshake. */
    private suspend fun freshToken(): String? {
        val current = tokens.current() ?: return null
        val expiresAt = current.expiresAtEpochMillis ?: return current.accessToken
        if (expiresAt > clock.now().toEpochMilliseconds()) return current.accessToken
        return tokens.refresh(stale = current)?.accessToken
    }

    private suspend fun closeQuietly(socket: DefaultClientWebSocketSession) {
        try {
            socket.close()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (alreadyGone: Exception) {
            Log.d("WebSocket was already closed: ${alreadyGone.message}", TAG)
        }
    }

    private fun backoffMillis(attempt: Int): Long {
        val ceiling = min(BASE_DELAY_MILLIS shl attempt.coerceAtMost(MAX_SHIFT), MAX_DELAY_MILLIS)
        return ceiling - (ceiling * JITTER_FRACTION * Random.nextDouble()).toLong()
    }

    private class Connection(
        val path: suspend () -> String,
        val authenticated: Boolean,
        val headers: suspend () -> Map<String, String>,
    )

    private companion object {
        const val TAG = "WebSocket"
        const val MESSAGE_BUFFER = 64
        const val BASE_DELAY_MILLIS = 1_000L
        const val MAX_DELAY_MILLIS = 30_000L
        const val MAX_SHIFT = 5
        const val JITTER_FRACTION = 0.4
    }
}
