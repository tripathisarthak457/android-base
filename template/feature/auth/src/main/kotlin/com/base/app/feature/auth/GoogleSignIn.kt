package com.base.app.feature.auth

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
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

/**
 * "Continue with Google", through Credential Manager.
 *
 * The button only fetches an ID token and hands it up. Exchanging it for a session is the
 * repository's job, like every other sign-in, so a Google account and a password end up in the
 * same token store through the same code.
 *
 * Needs the *web* client id from the Google Cloud console in `auth_google_server_client_id` —
 * the Android client id is the most common wrong answer and fails with a bare "developer error".
 * Until it is set, the button says so instead of opening a sheet that cannot work.
 */
@Composable
fun GoogleSignInButton(
    onIdToken: (String) -> Unit,
    onFailure: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val serverClientId = stringResource(R.string.auth_google_server_client_id)
    val notConfigured = stringResource(R.string.auth_google_not_configured)
    val failed = stringResource(R.string.auth_google_failed)
    var busy by remember { mutableStateOf(false) }

    AppButton(
        text = stringResource(R.string.auth_continue_with_google),
        onClick = {
            if (serverClientId.isBlank()) {
                onFailure(notConfigured)
                return@AppButton
            }
            scope.launch {
                busy = true
                when (val outcome = requestGoogleIdToken(context, serverClientId)) {
                    is GoogleOutcome.Token -> onIdToken(outcome.idToken)
                    GoogleOutcome.Cancelled -> Unit
                    GoogleOutcome.Failed -> onFailure(failed)
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
