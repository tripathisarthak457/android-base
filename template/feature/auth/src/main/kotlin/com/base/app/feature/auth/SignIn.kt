package com.base.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.common.AppResult
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.core.common.util.UiText
import com.base.app.core.common.validation.Validators
import com.base.app.core.common.validation.and
import com.base.app.core.designsystem.animation.busyOverlay
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppScaffold
// <opt:googlesignin>
import com.base.app.core.designsystem.component.container.AppDivider
// </opt:googlesignin>
import com.base.app.core.designsystem.component.feedback.AppBanner
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.input.AppPasswordField
import com.base.app.core.designsystem.component.input.AppTextField
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
data class SignInState(
    val error: UiText? = null,
) : UiState

sealed interface SignInEvent : UiEvent {
    data object Submit : SignInEvent
    data object CreateAccountClicked : SignInEvent
    data object ForgotPasswordClicked : SignInEvent
    // <opt:googlesignin>
    data class GoogleIdTokenReceived(val idToken: String) : SignInEvent
    data class GoogleFailed(val message: String) : SignInEvent
    // </opt:googlesignin>
}

sealed interface SignInEffect : UiEffect {
    data object SignedIn : SignInEffect
    data object OpenSignUp : SignInEffect
    data object OpenPasswordReset : SignInEffect
}

/** Sign in. */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : MviViewModel<SignInState, SignInEvent, SignInEffect>(SignInState()) {

    val form: FormState = buildForm {
        field("email", validator = Validators.required() and Validators.email())
        field("password", validator = Validators.required())
    }

    override suspend fun handleEvent(event: SignInEvent) {
        when (event) {
            SignInEvent.Submit -> submit()
            SignInEvent.CreateAccountClicked -> emitEffect(SignInEffect.OpenSignUp)
            SignInEvent.ForgotPasswordClicked -> emitEffect(SignInEffect.OpenPasswordReset)
            // <opt:googlesignin>
            is SignInEvent.GoogleIdTokenReceived -> signInWithGoogle(event.idToken)
            is SignInEvent.GoogleFailed -> updateState { copy(error = UiText.Dynamic(event.message)) }
            // </opt:googlesignin>
        }
    }
    // <opt:googlesignin>

    private suspend fun signInWithGoogle(idToken: String) {
        updateState { copy(error = null) }
        when (val result = authRepository.signInWithGoogle(idToken)) {
            is AppResult.Success -> emitEffect(SignInEffect.SignedIn)
            is AppResult.Failure -> updateState {
                copy(error = UiText.Dynamic(result.message ?: "Could not sign you in with Google."))
            }
        }
    }
    // </opt:googlesignin>

    private suspend fun submit() {
        updateState { copy(error = null) }

        val result = form.submitting { values ->
            authRepository.signIn(
                email = values.getValue("email"),
                password = values.getValue("password"),
            )
        } ?: return

        when (result) {
            is AppResult.Success -> emitEffect(SignInEffect.SignedIn)

            is AppResult.Failure -> {
                form.applyServerErrors(result.fieldErrors)
                if (result.fieldErrors.isEmpty()) {
                    updateState {
                        copy(error = UiText.Dynamic(result.message ?: "Could not sign you in."))
                    }
                }
            }
        }
    }
}

@Composable
fun SignInRoute(
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
        SignInScreen(state = state, form = viewModel.form, onEvent = onEvent)
    }
}

@Composable
fun SignInScreen(
    state: SignInState,
    form: FormState,
    onEvent: (SignInEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val email = form["email"]
    val password = form["password"]

    AppScaffold(modifier = modifier) {
        // No top bar on this screen, so it carries the status-bar inset itself.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
        ) {
            AppText(
                text = stringResource(R.string.auth_welcome_back),
                modifier = Modifier.padding(top = AppTheme.spacing.xxl),
                style = AppTheme.typography.displaySmall,
                color = AppTheme.colors.contentPrimary,
            )

            state.error?.let {
                AppBanner(text = it.asString(), tone = AppTone.Error)
            }

            Column(
                modifier = Modifier.busyOverlay(form.isSubmitting),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                AppTextField(
                    value = email.value,
                    onValueChange = email::onChange,
                    modifier = Modifier.touchOnFocusLost(email),
                    label = stringResource(R.string.auth_email),
                    placeholder = stringResource(R.string.auth_email_placeholder),
                    error = email.error?.asString(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )
                AppPasswordField(
                    value = password.value,
                    onValueChange = password::onChange,
                    modifier = Modifier.touchOnFocusLost(password),
                    label = stringResource(R.string.auth_password),
                    error = password.error?.asString(),
                    keyboardActions = KeyboardActions(onDone = { onEvent(SignInEvent.Submit) }),
                )
            }

            AppButton(
                text = stringResource(R.string.auth_sign_in),
                onClick = { onEvent(SignInEvent.Submit) },
                loading = form.isSubmitting,
                fillWidth = true,
            )
            // <opt:googlesignin>

            OrDivider()
            GoogleSignInButton(
                onIdToken = { onEvent(SignInEvent.GoogleIdTokenReceived(it)) },
                onFailure = { onEvent(SignInEvent.GoogleFailed(it)) },
                enabled = !form.isSubmitting,
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
                    variant = ButtonVariant.Ghost,
                )
                AppButton(
                    text = stringResource(R.string.auth_forgot_password),
                    onClick = { onEvent(SignInEvent.ForgotPasswordClicked) },
                    variant = ButtonVariant.Ghost,
                )
            }
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
            style = AppTheme.typography.caption,
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
            form = buildForm {
                field("email")
                field("password")
            },
            onEvent = {},
        )
    }
}
