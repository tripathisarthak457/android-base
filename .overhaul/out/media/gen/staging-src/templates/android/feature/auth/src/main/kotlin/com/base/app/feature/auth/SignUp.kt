package com.base.app.feature.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.base.app.core.common.result.AppError
import com.base.app.core.common.result.AppResult
import com.base.app.core.common.validation.ValidationError
import com.base.app.core.common.validation.Validators
import com.base.app.core.common.validation.and
import com.base.app.core.designsystem.animation.busyOverlay
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.feedback.AppBanner
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.input.AppSecureField
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.component.navigation.AppTopBar
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.mvi.MviViewModel
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState
import com.base.app.core.mvi.UiText
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
import com.base.app.core.ui.asString
import com.base.app.core.ui.form.toUiText
import com.base.app.data.auth.api.AuthRepository
import com.base.app.data.auth.api.SignUpDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop
import javax.inject.Inject

/** The fields' text is not here: it lives in the ViewModel's `TextFieldState`s. */
@Immutable
internal data class SignUpState(
    val nameError: UiText? = null,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmError: UiText? = null,
    val canSubmit: Boolean = false,
    val submitting: Boolean = false,
    val error: UiText? = null,
) : UiState

internal sealed interface SignUpEvent : UiEvent {
    data object Submit : SignUpEvent
    data object BackClicked : SignUpEvent
}

internal sealed interface SignUpEffect : UiEffect {
    data object SignedUp : SignUpEffect
    data object NavigateBack : SignUpEffect
}

@HiltViewModel
internal class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<SignUpState, SignUpEvent, SignUpEffect>(SignUpState()) {

    @OptIn(SavedStateHandleSaveableApi::class)
    val name: TextFieldState = savedStateHandle.saveable(NAME, saver = TextFieldState.Saver) { TextFieldState() }

    @OptIn(SavedStateHandleSaveableApi::class)
    val email: TextFieldState = savedStateHandle.saveable(EMAIL, saver = TextFieldState.Saver) { TextFieldState() }

    // Not saved: a password does not belong in the bundle the system writes to disk on process death.
    val password = TextFieldState()
    val confirm = TextFieldState()

    private val confirmRule = Validators.required() and Validators.sameAs { password.text }

    // Errors stay quiet until the first submit, then follow the text as it is corrected.
    private var showErrors = false

    init {
        // A rule runs only when its text changes. A collection restarted after the screen was away
        // starts with the text already judged, and judging it again would replace the error the
        // server returned for it.
        observe(snapshotFlow { name.text.toString() }.drop(1)) {
            copy(nameError = nameRule.errorFor(it, showErrors))
        }
        observe(snapshotFlow { email.text.toString() }.drop(1)) {
            copy(emailError = emailRule.errorFor(it, showErrors))
        }
        observe(snapshotFlow { password.text.toString() }.drop(1)) {
            copy(passwordError = passwordRule.errorFor(it, showErrors))
        }
        // Keyed on both, so changing the password re-checks the confirmation too.
        observe(snapshotFlow { confirm.text.toString() to password.text.toString() }.drop(1)) { (text, _) ->
            copy(confirmError = confirmError(text))
        }
        observe(snapshotFlow { listOf(name, email, password, confirm).all { it.text.isNotBlank() } }) {
            copy(canSubmit = it)
        }
    }

    override fun handleEvent(event: SignUpEvent) {
        when (event) {
            SignUpEvent.Submit -> submit()
            SignUpEvent.BackClicked -> emit(SignUpEffect.NavigateBack)
        }
    }

    private fun submit() {
        showErrors = true
        val nameError = nameRule.errorFor(name.text, showErrors)
        val emailError = emailRule.errorFor(email.text, showErrors)
        val passwordError = passwordRule.errorFor(password.text, showErrors)
        val confirmError = confirmError(confirm.text)
        setState {
            copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmError = confirmError,
                error = null,
            )
        }
        if (nameError != null || emailError != null || passwordError != null || confirmError != null) return

        launchExclusive(SUBMIT) {
            setState { copy(submitting = true) }
            val result = authRepository.signUp(
                SignUpDetails(
                    name = name.text.toString(),
                    email = email.text.toString(),
                    password = password.text.toString(),
                ),
            )
            setState { copy(submitting = false) }
            when (result) {
                is AppResult.Success -> emit(SignUpEffect.SignedUp)
                is AppResult.Failure -> showFailure(result.error, R.string.auth_sign_up_failed)
            }
        }
    }

    /** "Passwords do not match" says more than the generic mismatch message does. */
    private fun confirmError(text: CharSequence): UiText? {
        if (!showErrors) return null
        return when (val error = confirmRule.validate(text)) {
            ValidationError.Mismatch -> UiText.of(R.string.auth_passwords_do_not_match)
            else -> error?.toUiText()
        }
    }

    /** A validation response lands on the fields it names; anything else goes in the banner. */
    private fun showFailure(error: AppError, @StringRes fallback: Int) {
        val nameError = error.fieldError(NAME)
        val emailError = error.fieldError(EMAIL)
        val passwordError = error.fieldError(PASSWORD)
        setState {
            if (nameError == null && emailError == null && passwordError == null) {
                copy(error = error.authMessage(fallback))
            } else {
                copy(nameError = nameError, emailError = emailError, passwordError = passwordError)
            }
        }
    }

    private companion object {
        // The names the server's validation errors use, and the saved-state keys.
        const val NAME = "name"
        const val EMAIL = "email"
        const val PASSWORD = "password"
        const val SUBMIT = "submit"
        const val MIN_NAME_LENGTH = 2

        val nameRule = Validators.required() and Validators.minLength(MIN_NAME_LENGTH)
        val emailRule = Validators.required() and Validators.email()
        val passwordRule = Validators.password()
    }
}

@Composable
internal fun SignUpRoute(
    navigator: AppNavigator,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                // As with sign-in, the new session itself moves the app on.
                SignUpEffect.SignedUp -> Unit
                SignUpEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        SignUpScreen(
            state = state,
            name = viewModel.name,
            email = viewModel.email,
            password = viewModel.password,
            confirm = viewModel.confirm,
            onEvent = onEvent,
        )
    }
}

@Composable
internal fun SignUpScreen(
    state: SignUpState,
    name: TextFieldState,
    email: TextFieldState,
    password: TextFieldState,
    confirm: TextFieldState,
    onEvent: (SignUpEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFrame(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.auth_create_account),
                onBack = { onEvent(SignUpEvent.BackClicked) },
            )
        },
    ) {
        state.error?.let {
            AppBanner(text = it.asString(), tone = AppTone.Error)
        }

        Column(
            modifier = Modifier.busyOverlay(state.submitting),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            AppTextField(
                state = name,
                label = stringResource(R.string.auth_name),
                error = state.nameError?.asString(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            AppTextField(
                state = email,
                label = stringResource(R.string.auth_email),
                placeholder = stringResource(R.string.auth_email_placeholder),
                error = state.emailError?.asString(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            AppSecureField(
                state = password,
                label = stringResource(R.string.auth_password),
                helper = stringResource(R.string.auth_password_rules),
                error = state.passwordError?.asString(),
                imeAction = ImeAction.Next,
            )
            AppSecureField(
                state = confirm,
                label = stringResource(R.string.auth_confirm_password),
                error = state.confirmError?.asString(),
                onKeyboardAction = { onEvent(SignUpEvent.Submit) },
            )
        }

        AppButton(
            text = stringResource(R.string.auth_create_account),
            onClick = { onEvent(SignUpEvent.Submit) },
            enabled = state.canSubmit,
            loading = state.submitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpPreview() {
    AppTheme {
        SignUpScreen(
            state = SignUpState(),
            name = rememberTextFieldState(),
            email = rememberTextFieldState(),
            password = rememberTextFieldState(),
            confirm = rememberTextFieldState(),
            onEvent = {},
        )
    }
}
