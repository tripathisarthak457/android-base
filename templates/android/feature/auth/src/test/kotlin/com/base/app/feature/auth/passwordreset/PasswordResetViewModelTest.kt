package com.base.app.feature.auth.passwordreset

import android.annotation.SuppressLint
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.base.app.core.common.result.AppError
import com.base.app.core.common.result.AppResult
import com.base.app.core.mvi.UiText
import com.base.app.core.testing.MainDispatcherRule
import com.base.app.core.ui.R as UiR
import com.base.app.data.auth.api.FakeAuthRepository
import com.base.app.feature.auth.R
import com.base.app.feature.auth.ROBOLECTRIC_SDK
import com.base.app.feature.auth.type
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class PasswordResetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    @Test
    fun `a valid address is sent and the screen says a link is on its way`() = runTest {
        val viewModel = filledIn("ada@example.com")

        viewModel.onEvent(PasswordResetEvent.Submit)
        advanceUntilIdle()

        assertEquals(listOf("ada@example.com"), repository.resetRequests)
        assertTrue(viewModel.state.value.sent)
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun `the form can be submitted only once the address has text`() = runTest {
        val viewModel = watched()
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.email.type("ada@example.com")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `an invalid address is reported on submit and nothing is sent`() = runTest {
        val viewModel = filledIn("not an address")

        viewModel.onEvent(PasswordResetEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.of(UiR.string.ui_validation_email), viewModel.state.value.emailError)
        assertEquals(emptyList<String>(), repository.resetRequests)
        assertFalse(viewModel.state.value.sent)
    }

    @Test
    fun `after a submit the error follows the address as it is corrected`() = runTest {
        val viewModel = filledIn("not an address")
        viewModel.onEvent(PasswordResetEvent.Submit)
        advanceUntilIdle()

        viewModel.email.type("ada@example.com")
        advanceUntilIdle()

        assertNull(viewModel.state.value.emailError)
    }

    @Test
    fun `a 422 puts the server's message on the address`() = runTest {
        repository.result = AppResult.Failure(
            AppError.Http(status = 422, fieldErrors = mapOf("email" to listOf(UNDELIVERABLE))),
        )
        val viewModel = filledIn("ada@example.com")

        viewModel.onEvent(PasswordResetEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.Dynamic(UNDELIVERABLE), viewModel.state.value.emailError)
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.sent)
    }

    @Test
    fun `any other failure goes in the banner`() = runTest {
        repository.result = AppResult.Failure(AppError.Http(status = 503))
        val viewModel = filledIn("ada@example.com")

        viewModel.onEvent(PasswordResetEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.of(R.string.auth_reset_failed), viewModel.state.value.error)
        assertFalse(viewModel.state.value.sent)
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun `no connection says so, not that the link failed`() = runTest {
        repository.result = AppResult.Failure(AppError.Offline)
        val viewModel = filledIn("ada@example.com")

        viewModel.onEvent(PasswordResetEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.of(UiR.string.ui_error_offline), viewModel.state.value.error)
    }

    @Test
    fun `the form is busy while in flight, and a second submit sends nothing`() = runTest {
        val answer = CompletableDeferred<Unit>()
        repository.gate = answer
        val viewModel = filledIn("ada@example.com")

        viewModel.onEvent(PasswordResetEvent.Submit)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.submitting)
        viewModel.onEvent(PasswordResetEvent.Submit)
        answer.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.resetRequests.size)
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun `a request that throws leaves the form usable`() = runTest {
        repository.thrown = IllegalStateException("A bug in the repository")
        val viewModel = filledIn("ada@example.com")

        viewModel.onEvent(PasswordResetEvent.Submit)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.submitting)

        repository.thrown = null
        viewModel.onEvent(PasswordResetEvent.Submit)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.sent)
    }

    @Test
    fun `back asks to leave the screen`() = runTest {
        val viewModel = PasswordResetViewModel(repository, SavedStateHandle())

        viewModel.effects.test {
            viewModel.onEvent(PasswordResetEvent.BackClicked)
            assertEquals(PasswordResetEffect.NavigateBack, awaitItem())
        }
    }

    /**
     * A form the screen is showing, so the field observers run. runCurrent, not advanceUntilIdle:
     * with only the screen's background collector queued, the latter returns before it subscribes.
     */
    private fun TestScope.watched(): PasswordResetViewModel =
        PasswordResetViewModel(repository, SavedStateHandle()).also {
            it.state.launchIn(backgroundScope)
            runCurrent()
        }

    private fun TestScope.filledIn(email: String): PasswordResetViewModel =
        watched().apply { this.email.type(email) }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class PasswordResetProcessDeathTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // savedStateProvider() and createHandle() are what the framework calls on the way down and
    // back up; calling them here is the process death, without an Activity to kill.
    @SuppressLint("RestrictedApi")
    @Test
    fun `the address survives process death`() {
        val handle = SavedStateHandle()
        PasswordResetViewModel(FakeAuthRepository(), handle).email.type("ada@example.com")

        val saved = handle.savedStateProvider().saveState()
        val restored =
            PasswordResetViewModel(FakeAuthRepository(), SavedStateHandle.createHandle(saved, null))

        assertEquals("ada@example.com", restored.email.text.toString())
    }
}

private const val UNDELIVERABLE = "That address cannot receive mail"
