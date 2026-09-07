package com.base.app.lock

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.base.app.R
import com.base.app.core.datastore.AppSettings
import com.base.app.core.datastore.AppSettingsStore
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.ui.themeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The unlock screen, shown over the app until the user proves who they are.
 *
 * ## Why this is a second Activity
 *
 * `androidx.biometric` hosts its prompt in a Fragment, so it needs a `FragmentActivity`. Keeping
 * the lock here is what lets the app's only real Activity stay a plain `ComponentActivity` — no
 * fragment manager, and no `androidx.fragment` in the graph of a project that did not ask for a
 * lock.
 *
 * It also gets the behaviour right without any work. The lock has to cover the app completely,
 * survive rotation and refuse to be dismissed; a separate Activity that `finish()`es on success
 * is all three at once, where an overlay composable inside the app is a standing invitation to
 * find the state that renders underneath it.
 *
 * ## Back leaves rather than dismisses
 *
 * The default would return the user to the screen this is covering. Finishing the task instead
 * means the only way past this Activity is through it.
 */
@AndroidEntryPoint
class LockActivity : FragmentActivity() {

    @Inject
    lateinit var appLock: AppLock

    @Inject
    lateinit var settingsStore: AppSettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this) { finishAffinity() }

        setContent {
            val settings by settingsStore.settings.collectAsState(initial = AppSettings())
            AppTheme(mode = settings.themeMode()) {
                LockScreen(onUnlock = ::prompt)
            }
        }

        prompt()
    }

    private fun prompt() {
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    appLock.unlocked()
                    finish()
                }

                // Deliberately does nothing. An error here — too many attempts, or the user
                // dismissing the sheet — leaves this Activity up with its own Unlock button, so
                // the app stays locked and they can try again when they are ready.
                override fun onAuthenticationError(code: Int, message: CharSequence) = Unit
            },
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_lock_title))
                .setSubtitle(getString(R.string.app_lock_subtitle))
                .setAllowedAuthenticators(AppLock.allowedAuthenticators())
                .build(),
        )
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIcon(
            imageVector = AppIcons.Lock,
            contentDescription = null,
            tint = AppTheme.colors.contentTertiary,
            size = 48.dp,
        )
        AppText(
            text = stringResource(R.string.app_lock_title),
            style = AppTheme.typography.headingMedium,
            color = AppTheme.colors.contentPrimary,
            modifier = Modifier.padding(top = AppTheme.spacing.lg),
        )
        AppButton(
            text = stringResource(R.string.app_lock_action),
            onClick = onUnlock,
            modifier = Modifier.padding(top = AppTheme.spacing.xl),
        )
    }
}
