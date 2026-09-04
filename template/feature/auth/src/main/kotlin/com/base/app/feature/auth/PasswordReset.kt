package com.base.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.common.AppResult
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.core.common.util.UiText
import com.base.app.core.common.validation.Validators
import com.base.app.core.common.validation.and
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppBanner
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.component.navigation.AppBackTopBar
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
import com.base.app.core.ui.asString
import com.base.app.core.ui.form.FormState
import com.base.app.core.ui.form.buildForm
import com.base.app.core.ui.form.submitting
import com.base.app.core.ui.form.touchOnFocusLost
import com.base.app.data.auth.AuthRepository
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
 * Request a password-reset link.
 *
 * Success says a link *has been sent if the address is registered*, never whether it was. Telling
 * the user "no account with that email" turns the form into a way for anyone to test whether a
 * given person has an account here, which is a disclosure the reset flow gets nothing for.
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
                        copy(error = UiText.Dynamic(result.message ?: "Could not send the link."))
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

    AppScaffold(
        modifier = modifier,
        topBar = {
            AppBackTopBar(
                title = "Reset password",
                onBack = { onEvent(PasswordResetEvent.BackClicked) },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(AppTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
        ) {
            if (state.sent) {
                AppBanner(
                    text = "If that address has an account, a reset link is on its way.",
                    tone = AppTone.Success,
                )
            }

            AppText(
                text = "Enter the email you signed up with and we will send you a link.",
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
                label = "Email",
                placeholder = "you@example.com",
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
                text = "Send reset link",
                onClick = { onEvent(PasswordResetEvent.Submit) },
                loading = form.isSubmitting,
                fillWidth = true,
            )
        }
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
