package com.example.finalafter.crui.feature.auth.signup

import android.annotation.SuppressLint
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.finalafter.crui.core.common.result.AppError
import com.example.finalafter.crui.core.common.result.AppResult
import com.example.finalafter.crui.core.mvi.UiText
import com.example.finalafter.crui.core.testing.MainDispatcherRule
import com.example.finalafter.crui.core.ui.R as UiR
import com.example.finalafter.crui.data.auth.api.FakeAuthRepository
import com.example.finalafter.crui.data.auth.api.SignUpDetails
import com.example.finalafter.crui.feature.auth.R
import com.example.finalafter.crui.feature.auth.ROBOLECTRIC_SDK
import com.example.finalafter.crui.feature.auth.type
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

class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    @Test
    fun `a valid form sends what was typed and emits SignedUp`() = runTest {
        val viewModel = filledIn()

        viewModel.effects.test {
            viewModel.onEvent(SignUpEvent.Submit)
            assertEquals(SignUpEffect.SignedUp, awaitItem())
        }
        assertEquals(
            listOf(SignUpDetails(name = "Ada", email = "ada@example.com", password = "secret12")),
            repository.signUps,
        )
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun `the form can be submitted only once every field has text`() = runTest {
        val viewModel = watched()
        viewModel.name.type("Ada")
        viewModel.email.type("ada@example.com")
        viewModel.password.type("secret12")
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.confirm.type("secret12")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `nothing is judged until the first submit`() = runTest {
        val viewModel = watched()

        viewModel.email.type("not an address")
        advanceUntilIdle()

        assertNull(viewModel.state.value.emailError)
    }

    @Test
    fun `an invalid field is reported on submit and nothing is sent`() = runTest {
        val viewModel = filledIn(email = "not an address")

        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.of(UiR.string.ui_validation_email), viewModel.state.value.emailError)
        assertEquals(emptyList<SignUpDetails>(), repository.signUps)
    }

    @Test
    fun `a short name and a weak password are each reported on their own field`() = runTest {
        val viewModel = watched().apply {
            name.type("A")
            email.type("ada@example.com")
            password.type("letters only")
            confirm.type("letters only")
        }

        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.plural(UiR.plurals.ui_validation_min_length, 2), viewModel.state.value.nameError)
        assertEquals(UiText.of(UiR.string.ui_validation_password_digit), viewModel.state.value.passwordError)
        assertEquals(emptyList<SignUpDetails>(), repository.signUps)
    }

    @Test
    fun `after a submit each error follows the text as it is corrected`() = runTest {
        val viewModel = filledIn(email = "not an address")
        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()

        viewModel.email.type("ada@example.com")
        advanceUntilIdle()

        assertNull(viewModel.state.value.emailError)
    }

    @Test
    fun `a confirmation that differs says the passwords do not match`() = runTest {
        val viewModel = filledIn(confirm = "secret13")

        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()

        assertEquals(
            UiText.of(R.string.auth_passwords_do_not_match),
            viewModel.state.value.confirmError,
        )
        assertEquals(emptyList<SignUpDetails>(), repository.signUps)
    }

    @Test
    fun `changing the password checks the confirmation again`() = runTest {
        val viewModel = filledIn(confirm = "secret13")
        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()

        viewModel.password.type("secret13")
        advanceUntilIdle()

        assertNull(viewModel.state.value.confirmError)
    }

    @Test
    fun `a 422 puts the server's messages on the fields it names`() = runTest {
        repository.result = AppResult.Failure(
            AppError.Http(status = 422, fieldErrors = mapOf("email" to listOf(TAKEN))),
        )
        val viewModel = filledIn()

        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.Dynamic(TAKEN), viewModel.state.value.emailError)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `any other failure goes in the banner`() = runTest {
        repository.result = AppResult.Failure(AppError.Http(status = 500))
        val viewModel = filledIn()

        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.of(R.string.auth_sign_up_failed), viewModel.state.value.error)
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun `too many attempts says to wait`() = runTest {
        repository.result = AppResult.Failure(AppError.Http(status = 429))
        val viewModel = filledIn()

        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()

        assertEquals(UiText.of(R.string.auth_too_many_attempts), viewModel.state.value.error)
    }

    @Test
    fun `a submit after a failure clears the banner`() = runTest {
        repository.result = AppResult.Failure(AppError.Http(status = 500))
        val viewModel = filledIn()
        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()

        repository.result = AppResult.Success(Unit)
        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `the form is busy while in flight, and a second submit sends nothing`() = runTest {
        val answer = CompletableDeferred<Unit>()
        repository.gate = answer
        val viewModel = filledIn()

        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.submitting)
        viewModel.onEvent(SignUpEvent.Submit)
        answer.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.signUps.size)
        assertFalse(viewModel.state.value.submitting)
    }

    @Test
    fun `a request that throws leaves the form usable`() = runTest {
        repository.thrown = IllegalStateException("A bug in the repository")
        val viewModel = filledIn()

        viewModel.onEvent(SignUpEvent.Submit)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.submitting)

        repository.thrown = null
        viewModel.effects.test {
            viewModel.onEvent(SignUpEvent.Submit)
            assertEquals(SignUpEffect.SignedUp, awaitItem())
        }
    }

    @Test
    fun `back asks to leave the screen`() = runTest {
        val viewModel = SignUpViewModel(repository, SavedStateHandle())

        viewModel.effects.test {
            viewModel.onEvent(SignUpEvent.BackClicked)
            assertEquals(SignUpEffect.NavigateBack, awaitItem())
        }
    }

    /**
     * A form the screen is showing, so the field observers run. runCurrent, not advanceUntilIdle:
     * with only the screen's background collector queued, the latter returns before it subscribes.
     */
    private fun TestScope.watched(): SignUpViewModel =
        SignUpViewModel(repository, SavedStateHandle()).also {
            it.state.launchIn(backgroundScope)
            runCurrent()
        }

    private fun TestScope.filledIn(
        email: String = "ada@example.com",
        confirm: String = "secret12",
    ): SignUpViewModel = watched().apply {
        name.type("Ada")
        this.email.type(email)
        password.type("secret12")
        this.confirm.type(confirm)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class SignUpProcessDeathTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // savedStateProvider() and createHandle() are what the framework calls on the way down and
    // back up; calling them here is the process death, without an Activity to kill.
    @SuppressLint("RestrictedApi")
    @Test
    fun `the name and email survive process death and neither password does`() {
        val handle = SavedStateHandle()
        SignUpViewModel(FakeAuthRepository(), handle).apply {
            name.type("Ada")
            email.type("ada@example.com")
            password.type("secret12")
            confirm.type("secret12")
        }

        val saved = handle.savedStateProvider().saveState()
        val restored =
            SignUpViewModel(FakeAuthRepository(), SavedStateHandle.createHandle(saved, null))

        assertEquals("Ada", restored.name.text.toString())
        assertEquals("ada@example.com", restored.email.text.toString())
        assertEquals("", restored.password.text.toString())
        assertEquals("", restored.confirm.text.toString())
    }
}

private const val TAKEN = "An account already uses this address"
