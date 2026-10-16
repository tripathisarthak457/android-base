package com.base.app.core.network.websocket

import com.base.app.core.common.util.AppLogger
import com.base.app.core.coroutines.ApplicationScope
import com.base.app.core.coroutines.IoDispatcher
import com.base.app.core.datastore.AuthTokenStore
import com.base.app.core.network.NetworkConfig
import com.base.app.core.network.di.PlainClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/** What the socket is doing, for a UI that shows a "reconnecting" indicator. */
sealed interface SocketState {
    data object Disconnected : SocketState
    data object Connecting : SocketState
    data object Connected : SocketState
    data class Failed(val cause: Throwable?, val willRetry: Boolean) : SocketState
}

/**
 * One long-lived WebSocket, with reconnection. Delays double from one second to a maximum of
 * thirty.
 */
@Singleton
class AppWebSocket @Inject constructor(
    @PlainClient private val client: HttpClient,
    private val config: NetworkConfig,
    private val tokenStore: AuthTokenStore,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private val _state = MutableStateFlow<SocketState>(SocketState.Disconnected)
    val state: StateFlow<SocketState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = MESSAGE_BUFFER)
    val messages: Flow<String> = _messages.asSharedFlow()

    private var connection: Job? = null
    private var session: DefaultClientWebSocketSession? = null

    /** Opens the socket, or does nothing if it is already open. */
    fun connect(path: String = "", authenticated: Boolean = true) {
        if (connection?.isActive == true) return

        connection = scope.launch(ioDispatcher) {
            var attempt = 0
            while (isActive) {
                _state.value = SocketState.Connecting
                try {
                    // Read before opening: the request builder below is not a suspending scope,
                    // and the token comes from an encrypted store behind a suspend function.
                    val token = if (authenticated) tokenStore.accessToken() else null
                    val opened = client.webSocketSession(config.resolvedWebSocketUrl + path) {
                        token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                    }
                    session = opened
                    attempt = 0
                    _state.value = SocketState.Connected

                    for (frame in opened.incoming) {
                        if (frame is Frame.Text) _messages.emit(frame.readText())
                    }

                    // Falling out of the loop means the server closed the socket. That is a
                    // reconnect, not an error — servers close idle connections routinely.
                    _state.value = SocketState.Disconnected
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    AppLogger.w("WebSocket failed", throwable = throwable)
                    _state.value = SocketState.Failed(throwable, willRetry = true)
                } finally {
                    session = null
                }

                if (!isActive) break
                delay(backoffMillis(attempt++))
            }
        }
    }

    suspend fun send(message: String): Boolean = withContext(ioDispatcher) {
        val active = session ?: return@withContext false
        runCatching { active.send(Frame.Text(message)) }.isSuccess
    }

    fun disconnect() {
        connection?.cancel()
        connection = null
        scope.launch(ioDispatcher) {
            runCatching { session?.close() }
            session = null
            _state.value = SocketState.Disconnected
        }
    }

    private fun backoffMillis(attempt: Int): Long {
        val exponential = BASE_DELAY_MILLIS * 2.0.pow(attempt)
        val capped = min(exponential, MAX_DELAY_MILLIS.toDouble())
        val jitter = capped * JITTER_FRACTION * Math.random()
        return (capped - jitter).toLong()
    }

    private companion object {
        const val MESSAGE_BUFFER = 64
        const val BASE_DELAY_MILLIS = 1_000L
        const val MAX_DELAY_MILLIS = 30_000L
        const val JITTER_FRACTION = 0.4
    }
}
