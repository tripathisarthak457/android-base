package com.example.cmp.core.applock

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.cmp.core.ui.PlaceholderScreen

/**
 * Covers [content] with the unlock screen while [lock] says so. The decision for the first frame is
 * made synchronously, so the app's own content is never drawn, even for a frame, while it is meant
 * to be covered. Every window of the app composes its own gate over the same [AppLock], so on a
 * large screen with two windows both are covered. The window's overlays (alerts, sheets, menus,
 * toasts), which draw above everything the app does, are withdrawn with it.
 *
 * The activity must be a [FragmentActivity], which `BiometricPrompt` runs in.
 */
@Composable
fun AppLockGate(lock: AppLock, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val locked by lock.locked.collectAsStateWithLifecycle(initialValue = lock.isLockedNow())

    Box(modifier) {
        // Kept composed underneath, so unlocking returns to exactly where the person was. While
        // covered it is hidden from accessibility services, which would read it aloud, and closed
        // to keyboard focus, which would let Tab and Enter reach it through the cover.
        val covered = if (locked) {
            Modifier
                .clearAndSetSemantics {}
                .focusProperties { onEnter = { cancelFocusChange() } }
                .focusGroup()
        } else {
            Modifier
        }
        Box(covered) {
            content()
        }
        if (locked) UnlockScreen(lock)
    }
}

@Composable
private fun UnlockScreen(lock: AppLock) {
    val activity = LocalActivity.current as? FragmentActivity
        ?: error("AppLockGate needs a FragmentActivity: BiometricPrompt runs in one.")
    val challenge by lock.challenges.collectAsStateWithLifecycle()
    var problem by rememberSaveable { mutableStateOf<String?>(null) }

    val title = stringResource(R.string.applock_title)
    val subtitle = stringResource(R.string.applock_subtitle)
    val currentLock by rememberUpdatedState(lock)

    // Made again with the activity after a rotation: the prompt itself survives it, and reports to
    // the callback registered last.
    val prompt = remember(activity) {
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    problem = null
                    currentLock.unlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    problem = when (errorCode) {
                        // Dismissed: the Unlock button is there for another try.
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED,
                        -> null

                        // The screen lock was removed while the prompt was up. Nobody could pass
                        // the challenge any more, and only the phone's owner can remove a screen lock.
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        -> {
                            currentLock.unlocked()
                            null
                        }

                        // Lockout and the rest: the system's own words say when to try again.
                        else -> errString.toString()
                    }
                }
            },
        )
    }
    val info = remember(title, subtitle) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AppLock.allowedAuthenticators())
            .build()
    }

    // Asked once per challenge. A rotation or a process restart while the prompt is up must not
    // stack a second prompt on the first.
    var promptedFor by rememberSaveable { mutableLongStateOf(NotPrompted) }
    val current = challenge
    if (current != null) {
        // Launched after the frame rather than run in it: authenticate adds the prompt's fragment and
        // executes the transaction there and then, work no frame should wait for.
        LaunchedEffect(current) {
            if (promptedFor != current) {
                promptedFor = current
                prompt.authenticate(info)
            }
        }
    }

    // Focus moves onto the cover and stays there: a field focused underneath would otherwise keep
    // the keyboard up and take what is typed.
    val focus = remember { FocusRequester() }
    SideEffect(focus) { focus.requestFocus() }

    // Back leaves the app as it is rather than going anywhere inside it.
    val back = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = back, onBackCompleted = { activity.moveTaskToBack(true) })

    // The cover takes every tap, so none reaches the app underneath; one asks again, as Unlock
    // would. The system's reason a try failed (a lockout, say) is what a screen reader announces.
    // It takes focus on a touch screen too, where a clickable alone cannot: a field underneath
    // would otherwise keep it, and the keyboard with it. Focus sits inside a group it cannot
    // leave, as Unlock does inside the drawn cover, so neither Tab nor a screen clearing focus
    // can take it away.
    val unlock = stringResource(R.string.applock_action)
    PlaceholderScreen(
        title = R.string.applock_title,
        modifier = Modifier
            .focusProperties { onExit = { cancelFocusChange() } }
            .focusGroup()
            .focusProperties { canFocus = true }
            .focusRequester(focus)
            .semantics { problem?.let { stateDescription = it } }
            .clickable(onClickLabel = unlock) { prompt.authenticate(info) },
    )
}

private const val NotPrompted = Long.MIN_VALUE
