package com.example.wp04.dlplay.feature.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.wp04.dlplay.core.common.log.Log
import com.example.wp04.dlplay.core.designsystem.component.button.AppButton
import com.example.wp04.dlplay.core.designsystem.component.button.ButtonVariant
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
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
                when (val outcome = requestGoogleIdToken(context, serverClientId)) {
                    is GoogleOutcome.Token -> onIdToken(outcome.idToken)
                    GoogleOutcome.Cancelled -> Unit
                    GoogleOutcome.Failed -> onFailure()
                }
                busy = false
            }
        },
        modifier = modifier,
        variant = ButtonVariant.Secondary,
        enabled = enabled,
        loading = busy,
        fillWidth = true,
    )
}

private sealed interface GoogleOutcome {
    data class Token(val idToken: String) : GoogleOutcome
    data object Cancelled : GoogleOutcome
    data object Failed : GoogleOutcome
}

private suspend fun requestGoogleIdToken(context: Context, serverClientId: String): GoogleOutcome {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
        .build()

    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleOutcome.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleOutcome.Failed
        }
    } catch (_: GetCredentialCancellationException) {
        // The user closed the sheet. That is an answer, not an error.
        GoogleOutcome.Cancelled
    } catch (_: GetCredentialException) {
        GoogleOutcome.Failed
    } catch (_: GoogleIdTokenParsingException) {
        GoogleOutcome.Failed
    }
}
