package com.base.app.feature.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
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
import com.base.app.core.common.validation.Validators
import com.base.app.core.common.validation.and
import com.base.app.core.designsystem.animation.busyOverlay
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonVariant
// <opt:googlesignin>
import com.base.app.core.designsystem.component.container.AppDivider
// </opt:googlesignin>
import com.base.app.core.designsystem.component.feedback.AppBanner
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.input.AppSecureField
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.mvi.MviViewModel
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState
import com.base.app.core.mvi.UiText
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
import com.base.app.core.ui.asString
import com.base.app.data.auth.api.AuthRepository
import com.base.app.feature.auth.api.PasswordResetKey
import com.base.app.feature.auth.api.SignUpKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop
import javax.inject.Inject

/** The fields' text is not here: it lives in the ViewModel's `TextFieldState`s. */
@Immutable
internal data class SignInState(
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val canSubmit: Boolean = false,
    val submitting: Boolean = false,
    val error: UiText? = null,
) : UiState

internal sealed interface SignInEvent : UiEvent {
    data object Submit : SignInEvent
    data object CreateAccountClicked : SignInEvent
    data object ForgotPasswordClicked : SignInEvent
    // <opt:googlesignin>
    data class GoogleIdTokenReceived(val idToken: String) : SignInEvent
    data object GoogleFailed : SignInEvent
    // </opt:googlesignin>
}

internal sealed interface SignInEffect : UiEffect {
    data object SignedIn : SignInEffect
    data object OpenSignUp : SignInEffect
    data object OpenPasswordReset : SignInEffect
}

@HiltViewModel
internal class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<SignInState, SignInEvent, SignInEffect>(SignInState()) {

    @OptIn(SavedStateHandleSaveableApi::class)
    val email: TextFieldState = savedStateHandle.saveable(EMAIL, saver = TextFieldState.Saver) { TextFieldState() }

    // Not saved: a password does not belong in the bundle the system writes to disk on process death.
    val password = TextFieldState()

    // Errors stay quiet until the first submit, then follow the text as it is corrected.
    private var showErrors = false

    init {
        // A rule runs only when its text changes. A collection restarted after the screen was away
        // starts with the text already judged, and judging it again would replace the error the
        // server returned for it.
        observe(snapshotFlow { email.text.toString() }.drop(1)) {
            copy(emailError = emailRule.errorFor(it, showErrors))
        }
        observe(snapshotFlow { password.text.toString() }.drop(1)) {
            copy(passwordError = passwordRule.errorFor(it, showErrors))
        }
        observe(snapshotFlow { email.text.isNotBlank() && password.text.isNotEmpty() }) { copy(canSubmit = it) }
    }

    override fun handleEvent(event: SignInEvent) {
        when (event) {
            SignInEvent.Submit -> submit()
            SignInEvent.CreateAccountClicked -> emit(SignInEffect.OpenSignUp)
            SignInEvent.ForgotPasswordClicked -> emit(SignInEffect.OpenPasswordReset)
            // <opt:googlesignin>
            is SignInEvent.GoogleIdTokenReceived -> signInWithGoogle(event.idToken)
            SignInEvent.GoogleFailed -> setState { copy(error = UiText.of(R.string.auth_google_failed)) }
            // </opt:googlesignin>
        }
    }

    private fun submit() {
        showErrors = true
        val emailError = emailRule.errorFor(email.text, showErrors)
        val passwordError = passwordRule.errorFor(password.text, showErrors)
        setState { copy(emailError = emailError, passwordError = passwordError, error = null) }
        if (emailError != null || passwordError != null) return

        launchExclusive(SUBMIT) {
            setState { copy(submitting = true) }
            val result = authRepository.signIn(email = email.text.toString(), password = password.text.toString())
            setState { copy(submitting = false) }
            when (result) {
                is AppResult.Success -> emit(SignInEffect.SignedIn)
                is AppResult.Failure -> showFailure(result.error, R.string.auth_sign_in_failed)
            }
        }
    }
    // <opt:googlesignin>

    private fun signInWithGoogle(idToken: String) {
        setState { copy(error = null) }
        launchExclusive(SUBMIT) {
            setState { copy(submitting = true) }
            val result = authRepository.signInWithGoogle(idToken)
            setState { copy(submitting = false) }
            when (result) {
                is AppResult.Success -> emit(SignInEffect.SignedIn)
                is AppResult.Failure -> showFailure(result.error, R.string.auth_google_sign_in_failed)
            }
        }
    }
    // </opt:googlesignin>

    /** A validation response lands on the fields it names; anything else goes in the banner. */
    private fun showFailure(error: AppError, @StringRes fallback: Int) {
        val emailError = error.fieldError(EMAIL)
        val passwordError = error.fieldError(PASSWORD)
        setState {
            if (emailError == null && passwordError == null) {
                copy(error = error.authMessage(fallback))
            } else {
                copy(emailError = emailError, passwordError = passwordError)
            }
        }
    }

    private companion object {
        // The names the server's validation errors use, and the saved-state key for the email.
        const val EMAIL = "email"
        const val PASSWORD = "password"
        const val SUBMIT = "submit"

        val emailRule = Validators.required() and Validators.email()
        val passwordRule = Validators.required()
    }
}

@Composable
internal fun SignInRoute(
    navigator: AppNavigator,
    onSignedIn: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                SignInEffect.SignedIn -> onSignedIn()
                SignInEffect.OpenSignUp -> navigator.navigate(SignUpKey)
                SignInEffect.OpenPasswordReset -> navigator.navigate(PasswordResetKey)
            }
        },
    ) { state, onEvent ->
        SignInScreen(state = state, email = viewModel.email, password = viewModel.password, onEvent = onEvent)
    }
}

/**
 * The design-system fields take a String, so each field's text is read from the ViewModel's
 * `TextFieldState` and written straight back; nothing is copied into the state.
 */
@Composable
internal fun SignInScreen(
    state: SignInState,
    email: TextFieldState,
    password: TextFieldState,
    onEvent: (SignInEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFrame(modifier = modifier) {
        AppText(
            text = stringResource(R.string.auth_welcome_back),
            modifier = Modifier.padding(top = AppTheme.spacing.xxl),
            style = AppTheme.typography.title2,
            color = AppTheme.colors.contentPrimary,
        )

        state.error?.let {
            AppBanner(text = it.asString(), tone = AppTone.Error)
        }

        Column(
            modifier = Modifier.busyOverlay(state.submitting),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
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
                error = state.passwordError?.asString(),
                onKeyboardAction = { onEvent(SignInEvent.Submit) },
            )
        }

        AppButton(
            text = stringResource(R.string.auth_sign_in),
            onClick = { onEvent(SignInEvent.Submit) },
            enabled = state.canSubmit,
            loading = state.submitting,
            modifier = Modifier.fillMaxWidth(),
        )
        // <opt:googlesignin>

        OrDivider()
        GoogleSignInButton(
            onIdToken = { onEvent(SignInEvent.GoogleIdTokenReceived(it)) },
            onFailure = { onEvent(SignInEvent.GoogleFailed) },
            enabled = !state.submitting,
        )
        // </opt:googlesignin>

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppButton(
                text = stringResource(R.string.auth_create_account),
                onClick = { onEvent(SignInEvent.CreateAccountClicked) },
                variant = ButtonVariant.Plain,
            )
            AppButton(
                text = stringResource(R.string.auth_forgot_password),
                onClick = { onEvent(SignInEvent.ForgotPasswordClicked) },
                variant = ButtonVariant.Plain,
            )
        }
    }
}

// <opt:googlesignin>
@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppDivider(modifier = Modifier.weight(1f))
        AppText(
            text = stringResource(R.string.auth_or),
            style = AppTheme.typography.caption1,
            color = AppTheme.colors.contentTertiary,
        )
        AppDivider(modifier = Modifier.weight(1f))
    }
}

// </opt:googlesignin>
@Preview(showBackground = true)
@Composable
private fun SignInPreview() {
    AppTheme {
        SignInScreen(
            state = SignInState(),
            email = rememberTextFieldState(),
            password = rememberTextFieldState(),
            onEvent = {},
        )
    }
}
