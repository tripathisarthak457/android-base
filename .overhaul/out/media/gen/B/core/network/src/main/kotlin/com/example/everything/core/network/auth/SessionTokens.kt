package com.example.everything.core.network.auth

import com.example.everything.core.datastore.session.AuthTokenStore
import com.example.everything.core.datastore.session.AuthTokens
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The one way the app renews its tokens: every request refused with a 401 comes here, and so does
 * the socket. One refresh runs at a time. A server that rotates refresh tokens revokes the whole
 * family when a spent one is presented again, so two refreshes racing with the same token would sign
 * the person out.
 */
@Singleton
class SessionTokens @Inject constructor(
    private val store: AuthTokenStore,
    private val refresher: TokenRefresher,
    private val events: SessionEvents,
) {
    private val lock = Mutex()

    /** The tokens to send now, or null when nobody is signed in. */
    suspend fun current(): AuthTokens? = store.current()

    /**
     * Tokens to replace [stale], the pair a request was refused with, or null when there are none to
     * send: nobody is signed in, or someone other than [stale]'s owner is, and a request made in one
     * person's session is never sent again with another's. When the same person's session has
     * already replaced [stale], the stored pair is the answer and the server is not asked again.
     *
     * Only the server refusing the refresh token ends the session. A rate limit, a timeout or a
     * server error says nothing about the session, and the next request tries again.
     */
    suspend fun refresh(stale: AuthTokens): AuthTokens? = lock.withLock {
        val current = store.current()?.takeIf { it.owner == stale.owner } ?: return@withLock null
        if (current.refreshToken != stale.refreshToken) return@withLock current
        when (val result = refresher.refresh(current.refreshToken)) {
            is TokenRefreshResult.Success -> {
                val renewed = current.copy(
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken,
                    expiresAtEpochMillis = result.expiresAtEpochMillis,
                )
                // A sign-out or a new sign-in while the call was out is the session now: the late
                // answer neither brings the old one back nor overwrites the new one.
                if (store.replace(expected = current, replacement = renewed)) {
                    renewed
                } else {
                    store.current()?.takeIf { it.owner == stale.owner }
                }
            }

            TokenRefreshResult.Refused -> {
                if (store.replace(expected = current, replacement = null)) events.notifyExpired()
                null
            }

            is TokenRefreshResult.Failed -> null
        }
    }
}
