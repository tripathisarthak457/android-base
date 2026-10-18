package com.example.wp04.base.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.wp04.base.core.designsystem.animation.busyOverlay
import com.example.wp04.base.core.designsystem.component.button.AppButton
import com.example.wp04.base.core.designsystem.component.feedback.AppBanner
import com.example.wp04.base.core.designsystem.component.feedback.AppTone
import com.example.wp04.base.core.designsystem.component.input.AppPasswordField
import com.example.wp04.base.core.designsystem.component.input.AppTextField
import com.example.wp04.base.core.designsystem.component.navigation.AppBackTopBar
import com.example.wp04.base.core.designsystem.theme.AppTheme
import com.example.wp04.base.core.navigation.AppNavigator
import com.example.wp04.base.core.ui.MviScreen
import com.example.wp04.base.core.ui.asString
import com.example.wp04.base.core.ui.form.FormState
import com.example.wp04.base.core.ui.form.buildForm
import com.example.wp04.base.core.ui.form.submitting
import com.example.wp04.base.core.ui.form.touchOnFocusLost
import com.example.wp04.base.data.auth.AuthRepository
import com.example.wp04.base.data.auth.SignUpDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Immutable
data class SignUpState(
    val error: UiText? = null,
) : UiState

sealed interface SignUpEvent : UiEvent {
    data object Submit : SignUpEvent
    data object BackClicked : SignUpEvent
}

sealed interface SignUpEffect : UiEffect {
    data object SignedUp : SignUpEffect
    data object NavigateBack : SignUpEffect
}

/** Create an account. */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : MviViewModel<SignUpState, SignUpEvent, SignUpEffect>(SignUpState()) {
    // `form` refers to itself in the confirm rule; safe because the rule runs after construction.
    val form: FormState = buildForm {
        field("name", validator = Validators.required() and Validators.minLength(MIN_NAME_LENGTH))
        field("email", validator = Validators.required() and Validators.email())
        field("password", validator = Validators.password())
        field(
            name = "confirm",
            validator = Validators.required() and
                Validators.sameAs({ form["password"].value }, UiText.of(R.string.auth_passwords_do_not_match)),
        )
    }

    override suspend fun handleEvent(event: SignUpEvent) {
        when (event) {
            SignUpEvent.Submit -> submit()
            SignUpEvent.BackClicked -> emitEffect(SignUpEffect.NavigateBack)
        }
    }

    private suspend fun submit() {
        updateState { copy(error = null) }

        val result = form.submitting { values ->
            authRepository.signUp(
                SignUpDetails(
                    name = values.getValue("name"),
                    email = values.getValue("email"),
                    password = values.getValue("password"),
                ),
            )
        } ?: return

        when (result) {
            is AppResult.Success -> emitEffect(SignUpEffect.SignedUp)

            is AppResult.Failure -> {
                form.applyServerErrors(result.fieldErrors)
                if (result.fieldErrors.isEmpty()) {
                    updateState {
                        copy(error = result.authMessage(R.string.auth_sign_up_failed))
                    }
                }
            }
        }
    }

    private companion object {
        const val MIN_NAME_LENGTH = 2
    }
}

@Composable
fun SignUpRoute(
    navigator: AppNavigator,
    onSignedUp: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                SignUpEffect.SignedUp -> onSignedUp()
                SignUpEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        SignUpScreen(state = state, form = viewModel.form, onEvent = onEvent)
    }
}

@Composable
fun SignUpScreen(
    state: SignUpState,
    form: FormState,
    onEvent: (SignUpEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = form["name"]
    val email = form["email"]
    val password = form["password"]
    val confirm = form["confirm"]

    AuthFrame(
        modifier = modifier,
        topBar = {
            AppBackTopBar(
                title = stringResource(R.string.auth_create_account),
                onBack = { onEvent(SignUpEvent.BackClicked) },
            )
        },
    ) {
        state.error?.let {
            AppBanner(text = it.asString(), tone = AppTone.Error)
        }

        Column(
            modifier = Modifier.busyOverlay(form.isSubmitting),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            AppTextField(
                value = name.value,
                onValueChange = name::onChange,
                modifier = Modifier.touchOnFocusLost(name),
                label = stringResource(R.string.auth_name),
                error = name.error?.asString(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
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
                helper = stringResource(R.string.auth_password_rules),
                error = password.error?.asString(),
                imeAction = ImeAction.Next,
            )
            AppPasswordField(
                value = confirm.value,
                onValueChange = confirm::onChange,
                modifier = Modifier.touchOnFocusLost(confirm),
                label = stringResource(R.string.auth_confirm_password),
                error = confirm.error?.asString(),
                keyboardActions = KeyboardActions(onDone = { onEvent(SignUpEvent.Submit) }),
            )
        }

        AppButton(
            text = stringResource(R.string.auth_create_account),
            onClick = { onEvent(SignUpEvent.Submit) },
            loading = form.isSubmitting,
            fillWidth = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpPreview() {
    AppTheme {
        SignUpScreen(
            state = SignUpState(),
            form = buildForm {
                field("name")
                field("email")
                field("password")
                field("confirm")
            },
            onEvent = {},
        )
    }
}
