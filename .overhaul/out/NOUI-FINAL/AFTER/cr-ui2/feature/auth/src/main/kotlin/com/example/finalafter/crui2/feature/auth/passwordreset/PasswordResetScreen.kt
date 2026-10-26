package com.example.finalafter.crui2.feature.auth.passwordreset

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
import com.example.finalafter.crui2.core.designsystem.component.button.AppButton
import com.example.finalafter.crui2.core.designsystem.component.feedback.AppBanner
import com.example.finalafter.crui2.core.designsystem.component.feedback.AppTone
import com.example.finalafter.crui2.core.designsystem.component.input.AppTextField
import com.example.finalafter.crui2.core.designsystem.component.navigation.AppTopBar
import com.example.finalafter.crui2.core.designsystem.component.text.AppText
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import com.example.finalafter.crui2.core.ui.asString
import com.example.finalafter.crui2.feature.auth.AuthFrame
import com.example.finalafter.crui2.feature.auth.R

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
