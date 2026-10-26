package com.example.cmp.feature.auth.signin

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
import com.example.cmp.core.designsystem.animation.busyOverlay
import com.example.cmp.core.designsystem.component.button.AppButton
import com.example.cmp.core.designsystem.component.button.ButtonVariant
import com.example.cmp.core.designsystem.component.container.AppDivider
import com.example.cmp.core.designsystem.component.feedback.AppBanner
import com.example.cmp.core.designsystem.component.feedback.AppTone
import com.example.cmp.core.designsystem.component.input.AppSecureField
import com.example.cmp.core.designsystem.component.input.AppTextField
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.ui.asString
import com.example.cmp.feature.auth.AuthFrame
import com.example.cmp.feature.auth.R

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

        OrDivider()
        GoogleSignInButton(
            onIdToken = { onEvent(SignInEvent.GoogleIdTokenReceived(it)) },
            onFailure = { onEvent(SignInEvent.GoogleFailed) },
            enabled = !state.submitting,
        )

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
