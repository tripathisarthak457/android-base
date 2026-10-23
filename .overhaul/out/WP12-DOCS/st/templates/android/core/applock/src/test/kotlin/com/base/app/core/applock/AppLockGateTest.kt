package com.base.app.core.applock

import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.datastore.settings.AppSettingsStore
import com.base.app.core.designsystem.component.overlay.AppAlert
import com.base.app.core.designsystem.component.overlay.LocalOverlayHost
import com.base.app.core.designsystem.component.overlay.ToastMessage
import com.base.app.core.designsystem.component.sheet.AppSheet
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * The cover is the lock: whatever the app shows underneath must be out of reach of a finger, a
 * keyboard and an accessibility service until the person unlocks.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class AppLockGateTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    // The settings live in memory and everything that collects them runs unconfined, so the lock is
    // settled on the test thread before the activity starts, with no file and no other thread to
    // wait for. Reading and writing the file is AppSettingsStoreTest's job.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private lateinit var activity: ActivityController<FragmentActivity>

    private var now = 1_000_000L
    private val field = FocusRequester()
    private var taps by mutableIntStateOf(0)
    private var contentFocused by mutableStateOf(false)
    private var contentEverFocused = false
    private lateinit var focusManager: FocusManager

    @After
    fun tearDown() {
        activity.pause().stop().destroy()
        scope.cancel()
    }

    @Test
    fun `while locked, the content is out of reach of touch, keyboard and accessibility`() {
        show(lockedApp())

        compose.onNodeWithTag(ContentTag).assertDoesNotExist()

        compose.onRoot().performTouchInput { click(center) }
        compose.runOnIdle { assertEquals("A tap reached the locked content.", 0, taps) }

        compose.runOnIdle {
            repeat(3) {
                focusManager.moveFocus(FocusDirection.Next)
                focusManager.moveFocus(FocusDirection.Previous)
                focusManager.moveFocus(FocusDirection.Down)
            }
        }
        compose.runOnIdle { assertFalse("Keyboard focus moved into the locked content.", contentEverFocused) }
    }

    @Test
    fun `a field under the cover never takes focus, however often it asks`() {
        show(lockedApp())

        compose.runOnIdle { field.requestFocus() }

        compose.runOnIdle { assertFalse("The locked content took focus.", contentEverFocused) }
    }

    @Test
    fun `a field focused when the app locks loses focus to the cover`() {
        val lock = lockedApp()
        lock.unlocked()
        show(lock)
        compose.runOnIdle { assertTrue(contentFocused) }

        lockAgain(lock)

        compose.onNodeWithTag(ContentTag).assertDoesNotExist()
        compose.runOnIdle { assertFalse("The field kept focus, and the keyboard with it.", contentFocused) }
    }

    // The content stays composed and may clear focus at any moment, as screens do when the keyboard
    // hides; someone on a hardware keyboard must not lose their place on Unlock to it.
    @Test
    fun `Tab reaches Unlock, and the content underneath cannot take focus off it`() {
        show(lockedApp())
        val unlock = compose.onNodeWithText(activity.get().getString(R.string.applock_action))

        compose.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        unlock.assertIsFocused()

        compose.runOnIdle { focusManager.clearFocus() }
        unlock.assertIsFocused()
    }

    @Test
    fun `unlocking hands the content back`() {
        val lock = lockedApp()
        show(lock)

        compose.runOnIdle { lock.unlocked() }

        compose.onNodeWithTag(ContentTag).assertExists()
        compose.onRoot().performTouchInput { click(center) }
        compose.runOnIdle {
            assertEquals(1, taps)
            field.requestFocus()
        }
        compose.runOnIdle { assertTrue(contentFocused) }
    }

    // What the person left up when the app locked: Settings' sign-out alert, a sheet and a toast
    // with an action. The window draws them above its content, so above the cover too, unless the
    // gate withdraws them.
    @Test
    fun `alerts, sheets and toasts left up when the app locks are out of reach under the cover`() {
        var confirmed = 0
        var dismissed = 0
        var undone = 0
        val lock = lockedApp()
        lock.unlocked()
        show(lock) {
            AppSheet(visible = true, onDismissRequest = { dismissed++ }) { AppText(SheetText) }
            // Shown after the sheet, so it is the top overlay, as an alert raised from a sheet is.
            AppAlert(
                visible = true,
                title = AlertTitle,
                confirmLabel = SignOut,
                onConfirm = { confirmed++ },
                onDismissRequest = { dismissed++ },
                dismissLabel = "Cancel",
                destructive = true,
            )
            val host = LocalOverlayHost.current
            LaunchedEffect(Unit) {
                val toast = ToastMessage(
                    text = ToastText,
                    actionLabel = "Undo",
                    onAction = { undone++ },
                    durationMillis = 600_000,
                )
                host?.toasts?.show(toast)
            }
        }
        compose.onNodeWithText(AlertTitle).assertIsDisplayed()
        compose.onNodeWithText(ToastText).assertIsDisplayed()
        val signOut = compose.onNodeWithText(SignOut).getUnclippedBoundsInRoot()

        lockAgain(lock)

        for (text in listOf(AlertTitle, SignOut, SheetText, ToastText, "Undo")) {
            compose.onNodeWithText(text).assertDoesNotExist()
        }
        compose.onRoot().performTouchInput {
            val x = (signOut.left + signOut.right).toPx() / 2
            click(Offset(x, (signOut.top + signOut.bottom).toPx() / 2))
        }
        compose.runOnIdle { activity.get().onBackPressedDispatcher.onBackPressed() }
        compose.runOnIdle {
            assertEquals("A tap reached the alert under the cover.", 0, confirmed)
            assertEquals("Back went to an overlay under the cover.", 0, dismissed)
        }
        val unlock = compose.onNodeWithText(activity.get().getString(R.string.applock_action))
        unlock.assertIsDisplayed()
        compose.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        unlock.assertIsFocused()

        compose.runOnIdle { lock.unlocked() }

        for (text in listOf(AlertTitle, SignOut, SheetText, ToastText)) {
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        compose.onNodeWithText(SignOut).performClick()
        compose.runOnIdle { assertEquals(1, confirmed) }
        assertEquals(0, undone)
    }

    @Test
    fun `a sheet opened under the cover takes neither focus nor Back`() {
        var sheet by mutableStateOf(false)
        var dismissed = 0
        var sheetFocused = false
        show(lockedApp()) {
            AppSheet(visible = sheet, onDismissRequest = { dismissed++ }) {
                AppText(
                    SheetText,
                    modifier = Modifier
                        .onFocusChanged { if (it.isFocused) sheetFocused = true }
                        .focusable(),
                )
            }
        }
        val unlock = compose.onNodeWithText(activity.get().getString(R.string.applock_action))
        compose.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        unlock.assertIsFocused()

        compose.runOnIdle { sheet = true }
        compose.onNodeWithText(SheetText).assertDoesNotExist()
        unlock.assertIsFocused()
        compose.runOnIdle {
            repeat(3) { focusManager.moveFocus(FocusDirection.Next) }
            activity.get().onBackPressedDispatcher.onBackPressed()
        }
        compose.runOnIdle {
            assertFalse("Focus reached the sheet under the cover.", sheetFocused)
            assertEquals("Back went to the sheet under the cover.", 0, dismissed)
        }
    }

    private fun lockAgain(lock: AppLock) {
        compose.runOnIdle {
            val owner = activity.get()
            lock.onStop(owner)
            now += AppLock.GRACE_MILLIS + 1
            lock.onStart(owner)
        }
    }

    private fun lockedApp(): AppLock {
        val settings = AppSettingsStore(MemoryPreferences(), ApplicationScope(scope))
        scope.launch { settings.setAppLockEnabled(true) }
        check(settings.current?.appLockEnabled == true) { "The lock setting must be in place before the app starts." }
        return AppLock(settings, deviceCanAuthenticate = { true }, clock = { now }, scope = ApplicationScope(scope))
    }

    // The content asks for focus as soon as it is composed, as a screen with a search or sign-in
    // field does. BiometricPrompt needs a FragmentActivity, which no test manifest declares;
    // Robolectric builds one without a declaration.
    private fun show(lock: AppLock, overlays: @Composable () -> Unit = {}) {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        activity.get().setContent {
            focusManager = LocalFocusManager.current
            AppTheme {
                AppLockGate(lock) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(ContentTag)
                            .pointerInput(Unit) { detectTapGestures { taps++ } }
                            .focusRequester(field)
                            .onFocusChanged {
                                contentFocused = it.isFocused
                                if (it.isFocused) contentEverFocused = true
                            }
                            .focusable(),
                    )
                    LaunchedEffect(Unit) { field.requestFocus() }
                    overlays()
                }
            }
        }
        compose.waitForIdle()
    }
}

private class MemoryPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}

private const val ContentTag = "locked_content"
private const val AlertTitle = "Sign out of this phone?"
private const val SignOut = "Sign out"
private const val SheetText = "Sheet with the account's details"
private const val ToastText = "Draft deleted"

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project builds with.
private const val ROBOLECTRIC_SDK = 35
