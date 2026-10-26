package com.example.cmp.feature.auth.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.cmp.core.designsystem.animation.busyOverlay
import com.example.cmp.core.designsystem.component.button.AppButton
import com.example.cmp.core.designsystem.component.feedback.AppBanner
import com.example.cmp.core.designsystem.component.feedback.AppTone
import com.example.cmp.core.designsystem.component.input.AppSecureField
import com.example.cmp.core.designsystem.component.input.AppTextField
import com.example.cmp.core.designsystem.component.navigation.AppTopBar
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.ui.asString
import com.example.cmp.feature.auth.AuthFrame
import com.example.cmp.feature.auth.R

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
