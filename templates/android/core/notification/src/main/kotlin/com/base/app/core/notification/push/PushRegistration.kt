package com.base.app.core.notification.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.common.log.Log
import com.base.app.core.common.session.SessionScopedStore
// <opt:auth>
import com.base.app.core.datastore.session.AuthTokenStore
// </opt:auth>
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
// <opt:auth>
import kotlinx.coroutines.flow.filter
// </opt:auth>
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Where this install's push address goes: the backend sends to it with FCM's `Message.fid`. */
fun interface PushTokenSink {
    suspend fun submit(installationId: String)
}

/**
 * Registers this install with FCM once, and hands its installation id to [PushTokenSink] whenever it
 * changes; FCM itself reports a change through `onRegistered`. Not at every launch: registering is a
 * network call, and the id rarely changes. Sign-out unregisters, so the next person on this phone
 * never receives the previous one's notifications.
 */
@Singleton
class PushRegistration @Inject internal constructor(
    private val sink: PushTokenSink,
    @PushStore private val store: DataStore<Preferences>,
    // <opt:auth>
    private val tokenStore: AuthTokenStore,
    // </opt:auth>
    private val scope: ApplicationScope,
) : SessionScopedStore {

    /** Called once, from `Application.onCreate`. */
    fun start() {
        scope.launch {
            // <opt:auth>
            // Only someone signed in has notifications to receive.
            tokenStore.isAuthenticated.filter { it }.collect { ensureRegistered() }
            // </opt:auth>
            // <opt-else:auth>
            //~ ensureRegistered()
            // </opt-else:auth>
        }
    }

    internal suspend fun onRegistered(installationId: String) {
        if (store.data.first()[SUBMITTED] == installationId) return
        attempt("submit the installation id") {
            sink.submit(installationId)
            store.edit { it[SUBMITTED] = installationId }
        }
    }

    override suspend fun clear() {
        attempt("unregister") { FirebaseMessaging.getInstance().unregister().await() }
        store.edit { it.clear() }
    }

    private suspend fun ensureRegistered() {
        if (store.data.first()[SUBMITTED] != null) return
        attempt("register") { FirebaseMessaging.getInstance().register().await() }
    }

    /** No Play services, or no network: push is a convenience, so a failure is logged and the app goes on. */
    private suspend fun attempt(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Log.w("Push could not $what", failure, TAG)
        }
    }

    private companion object {
        const val TAG = "Push"
        val SUBMITTED = stringPreferencesKey("submitted_installation_id")
    }
}

/** The installation id last handed to the sink, in a file of its own. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class PushStore
