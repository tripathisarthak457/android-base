package com.base.app.startup

import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.SavedStateHandle
import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.datastore.session.AuthTokenStore
import com.base.app.core.datastore.session.AuthTokens
import com.base.app.core.datastore.settings.AppSettingsStore
import com.base.app.core.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** The entry gates: nothing is decided before the launch hooks, and each new session gets a new generation. */
@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest {

    @get:Rule
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    private val settings = AppSettingsStore(FakePreferences(), ApplicationScope(CoroutineScope(main.testDispatcher)))
    private val tokens = FakeTokens()

    private fun startup(
        saved: SavedStateHandle = SavedStateHandle(),
        hooks: Set<LaunchHook> = emptySet(),
    ): StartupViewModel = StartupViewModel(settings, tokens, hooks, saved).also { it.onLaunch(Intent()) }

    @Test
    fun `nothing is decided until the launch hooks have run`() = runTest(main.testDispatcher) {
        settings.setOnboardingCompleted(true)
        val hookDone = CompletableDeferred<Unit>()
        val hook = object : LaunchHook {
            override suspend fun beforeStart(intent: Intent) = hookDone.await()
        }
        val viewModel = startup(hooks = setOf(hook))

        assertNull(viewModel.startup.value)

        hookDone.complete(Unit)
        assertEquals(EntryIdentity.SignedIn(1), viewModel.startup.value?.identity)
    }

    // <opt:auth>
    @Test
    fun `each sign-in after a sign-out is a new generation`() = runTest(main.testDispatcher) {
        settings.setOnboardingCompleted(true)
        val viewModel = startup()

        assertEquals(EntryIdentity.SignedIn(1), viewModel.startup.value?.identity)
        tokens.isAuthenticated.value = false
        assertEquals(EntryIdentity.SignedOut, viewModel.startup.value?.identity)
        tokens.isAuthenticated.value = true
        assertEquals(EntryIdentity.SignedIn(2), viewModel.startup.value?.identity)
    }

    @Test
    fun `a restored process keeps the generation its saved shell was keyed on`() = runTest(main.testDispatcher) {
        settings.setOnboardingCompleted(true)
        val saved = SavedStateHandle()
        startup(saved)
        tokens.isAuthenticated.value = false
        tokens.isAuthenticated.value = true

        val restored = startup(SavedStateHandle(saved.keys().associateWith { saved.get<Any>(it) }))

        assertEquals(EntryIdentity.SignedIn(2), restored.startup.value?.identity)
    }
    // </opt:auth>
}

private class FakePreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}

private class FakeTokens : AuthTokenStore {
    override val isAuthenticated = MutableStateFlow(true)

    // The launch decides on isAuthenticated alone and never reads the tokens themselves.
    override val tokens: Flow<AuthTokens?> = flowOf(null)

    override suspend fun save(tokens: AuthTokens) = Unit

    override suspend fun replace(expected: AuthTokens, replacement: AuthTokens?): Boolean = false

    override suspend fun clear() = Unit
}
