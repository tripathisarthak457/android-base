package com.base.app.feature.auth.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.base.app.core.designsystem.animation.busyOverlay
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.feedback.AppBanner
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.input.AppSecureField
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.ui.asString
import com.base.app.feature.auth.AuthFrame
import com.base.app.feature.auth.R
// <opt:googlesignin>
import com.base.app.core.designsystem.component.container.AppDivider
// </opt:googlesignin>

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
