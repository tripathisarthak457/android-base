package com.example.everything.feature.auth

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
import com.example.everything.core.common.result.AppResult
import com.example.everything.core.common.validation.Validators
import com.example.everything.core.common.validation.and
import com.example.everything.core.designsystem.component.button.AppButton
import com.example.everything.core.designsystem.component.feedback.AppBanner
import com.example.everything.core.designsystem.component.feedback.AppTone
import com.example.everything.core.designsystem.component.input.AppTextField
import com.example.everything.core.designsystem.component.navigation.AppTopBar
import com.example.everything.core.designsystem.component.text.AppText
import com.example.everything.core.designsystem.theme.AppTheme
import com.example.everything.core.mvi.MviViewModel
import com.example.everything.core.mvi.UiEffect
import com.example.everything.core.mvi.UiEvent
import com.example.everything.core.mvi.UiState
import com.example.everything.core.mvi.UiText
import com.example.everything.core.navigation.AppNavigator
import com.example.everything.core.ui.MviScreen
import com.example.everything.core.ui.asString
import com.example.everything.data.auth.api.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.drop

@Immutable
internal data class PasswordResetState(
    val emailError: UiText? = null,
    val canSubmit: Boolean = false,
    val submitting: Boolean = false,
    val error: UiText? = null,
    val sent: Boolean = false,
) : UiState

internal sealed interface PasswordResetEvent : UiEvent {
    data object Submit : PasswordResetEvent
    data object BackClicked : PasswordResetEvent
}

internal sealed interface PasswordResetEffect : UiEffect {
    data object NavigateBack : PasswordResetEffect
}

/**
 * Request a password-reset link. Success says a link *has been sent if the address is registered*,
 * never whether it was.
 */
@HiltViewModel
internal class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<PasswordResetState, PasswordResetEvent, PasswordResetEffect>(PasswordResetState()) {

    @OptIn(SavedStateHandleSaveableApi::class)
    val email: TextFieldState = savedStateHandle.saveable(EMAIL, saver = TextFieldState.Saver) { TextFieldState() }

    // Errors stay quiet until the first submit, then follow the text as it is corrected.
    private var showErrors = false

    init {
        // The rule runs only when the text changes. A collection restarted after the screen was away
        // starts with the text already judged, and judging it again would replace the error the
        // server returned for it.
        observe(snapshotFlow { email.text.toString() }.drop(1)) {
            copy(emailError = emailRule.errorFor(it, showErrors))
        }
        observe(snapshotFlow { email.text.isNotBlank() }) { copy(canSubmit = it) }
    }

    override fun handleEvent(event: PasswordResetEvent) {
        when (event) {
            PasswordResetEvent.Submit -> submit()
            PasswordResetEvent.BackClicked -> emit(PasswordResetEffect.NavigateBack)
        }
    }

    private fun submit() {
        showErrors = true
        val emailError = emailRule.errorFor(email.text, showErrors)
        setState { copy(emailError = emailError, error = null) }
        if (emailError != null) return

        launchExclusive(SUBMIT) {
            setState { copy(submitting = true) }
            val result = authRepository.requestPasswordReset(email.text.toString())
            setState { copy(submitting = false) }
            when (result) {
                is AppResult.Success -> setState { copy(sent = true) }
                is AppResult.Failure -> {
                    val fieldError = result.error.fieldError(EMAIL)
                    setState {
                        if (fieldError != null) {
                            copy(emailError = fieldError)
                        } else {
                            copy(error = result.error.authMessage(R.string.auth_reset_failed))
                        }
                    }
                }
            }
        }
    }

    private companion object {
        // The name the server's validation errors use, and the saved-state key.
        const val EMAIL = "email"
        const val SUBMIT = "submit"

        val emailRule = Validators.required() and Validators.email()
    }
}

@Composable
internal fun PasswordResetRoute(
    navigator: AppNavigator,
    viewModel: PasswordResetViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                PasswordResetEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        PasswordResetScreen(state = state, email = viewModel.email, onEvent = onEvent)
    }
}

@Composable
internal fun PasswordResetScreen(
    state: PasswordResetState,
    email: TextFieldState,
    onEvent: (PasswordResetEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthFrame(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.auth_reset_password),
                onBack = { onEvent(PasswordResetEvent.BackClicked) },
            )
        },
    ) {
        if (state.sent) {
            AppBanner(
                text = stringResource(R.string.auth_reset_sent),
                tone = AppTone.Success,
            )
        }

        AppText(
            text = stringResource(R.string.auth_reset_explanation),
            style = AppTheme.typography.callout,
            color = AppTheme.colors.contentSecondary,
        )

        state.error?.let {
            AppBanner(text = it.asString(), tone = AppTone.Error)
        }

        AppTextField(
            state = email,
            label = stringResource(R.string.auth_email),
            placeholder = stringResource(R.string.auth_email_placeholder),
            error = state.emailError?.asString(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            onKeyboardAction = { onEvent(PasswordResetEvent.Submit) },
        )

        AppButton(
            text = stringResource(R.string.auth_send_reset_link),
            onClick = { onEvent(PasswordResetEvent.Submit) },
            enabled = state.canSubmit,
            loading = state.submitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordResetPreview() {
    AppTheme {
        PasswordResetScreen(
            state = PasswordResetState(sent = true),
            email = rememberTextFieldState(),
            onEvent = {},
        )
    }
}
