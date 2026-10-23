package com.example.standard.feature.auth

import android.annotation.SuppressLint
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.standard.core.common.result.AppError
import com.example.standard.core.common.result.AppResult
import com.example.standard.core.mvi.UiText
import com.example.standard.core.testing.MainDispatcherRule
import com.example.standard.core.ui.R as UiR
import com.example.standard.data.auth.api.AuthRepository
import com.example.standard.data.auth.api.SignUpDetails
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    @Test
    fun `a successful sign-in emits SignedIn`() = runTest {
        val viewModel = filledIn()

        viewModel.effects.test {
            viewModel.onEvent(SignInEvent.Submit)
            assertEquals(SignInEffect.SignedIn, awaitItem())
        }
        assertEquals(listOf("ada@example.com" to "secret"), repository.signIns)
    }

    @Test
    fun `a 401 says the email and password do not match`() = runTest {
        repository.result = AppResult.Failure(AppError.Http(status = 401))
        val viewModel = filledIn()

        viewModel.onEvent(SignInEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.of(R.string.auth_wrong_credentials), viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.submitting)
    }

    @Test
    fun `a 422 puts the server's messages on the fields it names`() = runTest {
        repository.result = AppResult.Failure(
            AppError.Http(status = 422, fieldErrors = mapOf("email" to listOf("No account uses this address"))),
        )
        val viewModel = filledIn()

        viewModel.onEvent(SignInEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.Dynamic("No account uses this address"), viewModel.state.value.emailError)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `the server's message on a field outlasts the screen going away`() = runTest {
        repository.result = AppResult.Failure(
            AppError.Http(status = 422, fieldErrors = mapOf("email" to listOf("No account uses this address"))),
        )
        val viewModel = filledIn()
        val screen = viewModel.state.launchIn(backgroundScope)
        runCurrent()

        viewModel.onEvent(SignInEvent.Submit)
        advanceUntilIdle()
        screen.cancel()
        // Past the grace period, so the field observers stop and start again when the screen returns.
        advanceTimeBy(6.seconds)
        viewModel.state.launchIn(backgroundScope)
        // runCurrent, not advanceUntilIdle: with only background work queued, the latter returns
        // before the new collector has subscribed.
        runCurrent()

        assertEquals(UiText.Dynamic("No account uses this address"), viewModel.state.value.emailError)
    }

    @Test
    fun `a second submit while the first is in flight sends nothing`() = runTest {
        val answer = CompletableDeferred<Unit>()
        repository.gate = answer
        val viewModel = filledIn()

        viewModel.onEvent(SignInEvent.Submit)
        advanceUntilIdle()
        viewModel.onEvent(SignInEvent.Submit)
        answer.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.signIns.size)
    }

    @Test
    fun `an invalid email is reported on the field and nothing is sent`() = runTest {
        val viewModel = SignInViewModel(repository, SavedStateHandle())
        viewModel.state.launchIn(backgroundScope)
        viewModel.email.type("not an address")
        viewModel.password.type("secret")

        viewModel.onEvent(SignInEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.of(UiR.string.ui_validation_email), viewModel.state.value.emailError)
        assertEquals(emptyList<Pair<String, String>>(), repository.signIns)
    }

    private fun filledIn(): SignInViewModel = SignInViewModel(repository, SavedStateHandle()).apply {
        email.type("ada@example.com")
        password.type("secret")
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class SignInProcessDeathTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // savedStateProvider() and createHandle() are what the framework calls on the way down and
    // back up; calling them here is the process death, without an Activity to kill.
    @SuppressLint("RestrictedApi")
    @Test
    fun `the email survives process death and the password does not`() {
        val handle = SavedStateHandle()
        SignInViewModel(FakeAuthRepository(), handle).apply {
            email.type("ada@example.com")
            password.type("secret")
        }

        val saved = handle.savedStateProvider().saveState()
        val restored = SignInViewModel(FakeAuthRepository(), SavedStateHandle.createHandle(saved, null))

        assertEquals("ada@example.com", restored.email.text.toString())
        assertEquals("", restored.password.text.toString())
    }
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project builds with.
private const val ROBOLECTRIC_SDK = 35

/** What a keyboard does to the field, plus the frame tick that tells snapshot observers about it. */
private fun TextFieldState.type(text: String) {
    edit { replace(0, length, text) }
    Snapshot.sendApplyNotifications()
}

private class FakeAuthRepository : AuthRepository {
    var result: AppResult<Unit> = AppResult.Success(Unit)
    var gate: CompletableDeferred<Unit>? = null
    val signIns = mutableListOf<Pair<String, String>>()

    override val isSignedIn = MutableStateFlow(false)

    override suspend fun signIn(email: String, password: String): AppResult<Unit> {
        signIns += email to password
        gate?.await()
        return result
    }

    override suspend fun signUp(details: SignUpDetails): AppResult<Unit> = result

    override suspend fun requestPasswordReset(email: String): AppResult<Unit> = result
}
