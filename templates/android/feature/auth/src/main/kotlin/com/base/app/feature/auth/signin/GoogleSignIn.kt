// Named for the request it makes; GoogleOutcome is only what the request comes to.
@file:Suppress("ktlint:standard:filename")

package com.base.app.feature.auth.signin

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** What asking Credential Manager for a Google ID token came to. */
internal sealed interface GoogleOutcome {
    data class Token(val idToken: String) : GoogleOutcome
    data object Cancelled : GoogleOutcome
    data object Failed : GoogleOutcome
}

/**
 * A Google ID token through Credential Manager's sheet, for the sign-in screen's "Continue with
 * Google" control to send as [SignInEvent.GoogleIdTokenReceived]. [serverClientId] is
 * `R.string.auth_google_server_client_id`, the OAuth web client id; blank until it is set up.
 */
internal suspend fun requestGoogleIdToken(context: Context, serverClientId: String): GoogleOutcome {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
        .build()

    return try {
        val credential = CredentialManager.create(
            context,
        ).getCredential(context, request).credential
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
