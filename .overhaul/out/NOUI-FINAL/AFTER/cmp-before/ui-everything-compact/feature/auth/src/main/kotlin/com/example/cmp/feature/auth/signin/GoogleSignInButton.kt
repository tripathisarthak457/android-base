package com.example.cmp.feature.auth.signin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.cmp.core.common.log.Log
import com.example.cmp.core.designsystem.component.button.AppButton
import com.example.cmp.core.designsystem.component.button.ButtonVariant
import com.example.cmp.feature.auth.R
import kotlinx.coroutines.launch

/** "Continue with Google", through Credential Manager. */
@Composable
internal fun GoogleSignInButton(
    onIdToken: (String) -> Unit,
    onFailure: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val serverClientId = stringResource(R.string.auth_google_server_client_id)
    var busy by remember { mutableStateOf(false) }

    AppButton(
        text = stringResource(R.string.auth_continue_with_google),
        onClick = {
            if (serverClientId.isBlank()) {
                // Said to the developer, not the person tapping: they get the ordinary failure.
                Log.w("Google sign-in is not set up: put the OAuth web client id in auth_google_server_client_id.")
                onFailure()
                return@AppButton
            }
            scope.launch {
                busy = true
                try {
                    when (val outcome = requestGoogleIdToken(context, serverClientId)) {
                        is GoogleOutcome.Token -> onIdToken(outcome.idToken)
                        GoogleOutcome.Cancelled -> Unit
                        GoogleOutcome.Failed -> onFailure()
                    }
                } finally {
                    busy = false
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        variant = ButtonVariant.Tinted,
        enabled = enabled,
        loading = busy,
    )
}
