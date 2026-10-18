package com.example.wp04.base.feature.auth

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.wp04.base.core.common.AppResult
import com.example.wp04.base.core.common.mvi.MviViewModel
import com.example.wp04.base.core.common.mvi.UiEffect
import com.example.wp04.base.core.common.mvi.UiEvent
import com.example.wp04.base.core.common.mvi.UiState
import com.example.wp04.base.core.common.util.UiText
import com.example.wp04.base.core.common.validation.Validators
import com.example.wp04.base.core.common.validation.and
import com.example.wp04.base.core.designsystem.component.button.AppButton
import com.example.wp04.base.core.designsystem.component.feedback.AppBanner
import com.example.wp04.base.core.designsystem.component.feedback.AppTone
import com.example.wp04.base.core.designsystem.component.input.AppTextField
import com.example.wp04.base.core.designsystem.component.navigation.AppBackTopBar
import com.example.wp04.base.core.designsystem.component.text.AppText
import com.example.wp04.base.core.designsystem.theme.AppTheme
import com.example.wp04.base.core.navigation.AppNavigator
import com.example.wp04.base.core.ui.MviScreen
import com.example.wp04.base.core.ui.asString
import com.example.wp04.base.core.ui.form.FormState
import com.example.wp04.base.core.ui.form.buildForm
import com.example.wp04.base.core.ui.form.submitting
import com.example.wp04.base.core.ui.form.touchOnFocusLost
import com.example.wp04.base.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Immutable
data class PasswordResetState(
    val error: UiText? = null,
    val sent: Boolean = false,
) : UiState

sealed interface PasswordResetEvent : UiEvent {
    data object Submit : PasswordResetEvent
    data object BackClicked : PasswordResetEvent
}

sealed interface PasswordResetEffect : UiEffect {
    data object NavigateBack : PasswordResetEffect
}

/**
 * Request a password-reset link. Success says a link *has been sent if the address is registered*,
 * never whether it was.
 */
@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : MviViewModel<PasswordResetState, PasswordResetEvent, PasswordResetEffect>(PasswordResetState()) {
    val form: FormState = buildForm {
        field("email", validator = Validators.required() and Validators.email())
    }

    override suspend fun handleEvent(event: PasswordResetEvent) {
        when (event) {
            PasswordResetEvent.Submit -> submit()
            PasswordResetEvent.BackClicked -> emitEffect(PasswordResetEffect.NavigateBack)
        }
    }

    private suspend fun submit() {
        updateState { copy(error = null) }

        val result = form.submitting { values ->
            authRepository.requestPasswordReset(values.getValue("email"))
        } ?: return

        when (result) {
            is AppResult.Success -> updateState { copy(sent = true) }

            is AppResult.Failure -> {
                form.applyServerErrors(result.fieldErrors)
                if (result.fieldErrors.isEmpty()) {
                    updateState {
                        copy(error = result.authMessage(R.string.auth_reset_failed))
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordResetRoute(
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
        PasswordResetScreen(state = state, form = viewModel.form, onEvent = onEvent)
    }
}

@Composable
fun PasswordResetScreen(
    state: PasswordResetState,
    form: FormState,
    onEvent: (PasswordResetEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val email = form["email"]

    AuthFrame(
        modifier = modifier,
        topBar = {
            AppBackTopBar(
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
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.contentSecondary,
        )

        state.error?.let {
            AppBanner(text = it.asString(), tone = AppTone.Error)
        }

        AppTextField(
            value = email.value,
            onValueChange = email::onChange,
            modifier = Modifier.touchOnFocusLost(email),
            label = stringResource(R.string.auth_email),
            placeholder = stringResource(R.string.auth_email_placeholder),
            error = email.error?.asString(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onEvent(PasswordResetEvent.Submit) },
            ),
        )

        AppButton(
            text = stringResource(R.string.auth_send_reset_link),
            onClick = { onEvent(PasswordResetEvent.Submit) },
            loading = form.isSubmitting,
            fillWidth = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PasswordResetPreview() {
    AppTheme {
        PasswordResetScreen(
            state = PasswordResetState(sent = true),
            form = buildForm { field("email") },
            onEvent = {},
        )
    }
}
