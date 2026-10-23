package com.example.videoalone.startup

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videoalone.core.datastore.session.AuthTokenStore
import com.example.videoalone.core.datastore.settings.AppSettings
import com.example.videoalone.core.datastore.settings.AppSettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the first frame depends on. The splash is held until all of it has arrived. */
data class Startup(
    val settings: AppSettings,
    val identity: EntryIdentity,
)

/**
 * Which app the person is in. Each is its own host, and moving between them is a host swap, never a
 * navigation: nothing pushes the sign-in screen into a tab, and signing in does not push the tabs
 * onto the sign-in stack.
 */
sealed interface EntryIdentity {
    data object Onboarding : EntryIdentity

    data object SignedOut : EntryIdentity

    /** [generation] changes with every new session, so the next person starts with fresh tabs. */
    data class SignedIn(val generation: Int) : EntryIdentity
}

/**
 * Reads the settings and the session once for the activity and keeps them across recreation, so a
 * rotation or a theme change has the entry point at once instead of a blank frame while DataStore
 * answers again.
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    settingsStore: AppSettingsStore,
    tokenStore: AuthTokenStore,
    private val hooks: Set<@JvmSuppressWildcards LaunchHook>,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val hooked = CompletableDeferred<Unit>()
    private var launching = false

    // Saved, so a process restored from the background keeps the generation its saved shell state
    // was keyed on.
    private var generation: Int = savedState[GENERATION] ?: 0
    private var signedIn: Boolean = savedState[SIGNED_IN] ?: false

    val startup: StateFlow<Startup?> = flow {
        hooked.await()
        emitAll(
            combine(settingsStore.settings, tokenStore.isAuthenticated) { settings, authenticated ->
                Startup(settings, identityFor(settings, authenticated))
            },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The activity's launch intent; the launch hooks see it before anything is decided. */
    fun onLaunch(intent: Intent) {
        if (launching) return
        launching = true
        viewModelScope.launch {
            hooks.forEach { it.beforeStart(intent) }
            hooked.complete(Unit)
        }
    }

    private fun identityFor(settings: AppSettings, authenticated: Boolean): EntryIdentity {
        if (!signedIn) {
            generation++
            record(signedIn = true)
        }
        return EntryIdentity.SignedIn(generation)
    }

    private fun record(signedIn: Boolean) {
        this.signedIn = signedIn
        savedState[SIGNED_IN] = signedIn
        savedState[GENERATION] = generation
    }

    private companion object {
        const val GENERATION = "session_generation"
        const val SIGNED_IN = "signed_in"
    }
}
