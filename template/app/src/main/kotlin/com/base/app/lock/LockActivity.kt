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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

/** The unlock screen, shown over the app until the user proves who they are. */
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
            val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
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

                // Nothing to do: the Unlock button stays on screen for another try.
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
