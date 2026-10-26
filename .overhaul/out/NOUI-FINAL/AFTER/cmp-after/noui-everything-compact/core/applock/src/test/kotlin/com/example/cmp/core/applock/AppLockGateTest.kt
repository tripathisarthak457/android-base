package com.example.cmp.core.applock

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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cmp.core.common.coroutines.ApplicationScope
import com.example.cmp.core.datastore.FakePreferencesDataStore
import com.example.cmp.core.datastore.settings.AppSettingsStore
import com.example.cmp.core.designsystem.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
        compose.runOnIdle {
            assertFalse("Keyboard focus moved into the locked content.", contentEverFocused)
        }
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
        compose.runOnIdle {
            assertFalse("The field kept focus, and the keyboard with it.", contentFocused)
        }
    }

    // The content stays composed and may clear focus at any moment, as screens do when the keyboard
    // hides; someone on a hardware keyboard must not lose their place on the cover to it.
    @Test
    fun `Tab reaches the cover, and the content underneath cannot take focus off it`() {
        show(lockedApp())
        val cover = compose.onNodeWithTag(PlaceholderTag)

        compose.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        cover.assertIsFocused()

        compose.runOnIdle { focusManager.clearFocus() }
        cover.assertIsFocused()
    }

    // Without an Unlock button the cover is the only way to ask again: a person who dismissed the
    // prompt would otherwise stay locked out until the process ends.
    @Test
    fun `a tap on the cover asks again, under Unlock's name, and reaches nothing else`() {
        show(lockedApp())
        val unlock = activity.get().getString(R.string.applock_action)
        val cover = compose.onNodeWithTag(PlaceholderTag)

        cover.assert(
            SemanticsMatcher("click action labelled \"$unlock\"") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == unlock
            },
        )
        cover.performClick()

        compose.runOnIdle { assertEquals("A tap reached the locked content.", 0, taps) }
        compose.onNodeWithTag(ContentTag).assertDoesNotExist()
    }

    // A phone is in touch mode, where a clickable alone cannot take focus: the cover must still take
    // it from a field underneath, or the keyboard stays up and typing reaches the locked content.
    @Test
    fun `on a touch screen too, a field focused when the app locks loses focus to the cover`() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
        try {
            lateinit var input: InputModeManager
            val lock = lockedApp()
            lock.unlocked()
            show(lock) { input = LocalInputModeManager.current }
            compose.runOnIdle {
                assertEquals(InputMode.Touch, input.inputMode)
                assertTrue(contentFocused)
            }

            lockAgain(lock)

            compose.runOnIdle {
                assertFalse("The field kept focus, and the keyboard with it.", contentFocused)
            }
            compose.onNodeWithTag(PlaceholderTag).assertIsFocused()
        } finally {
            InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        }
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

    private fun lockAgain(lock: AppLock) {
        compose.runOnIdle {
            val owner = activity.get()
            lock.onStop(owner)
            now += AppLock.GRACE_MILLIS + 1
            lock.onStart(owner)
        }
    }

    private fun lockedApp(): AppLock {
        val settings = AppSettingsStore(FakePreferencesDataStore(), ApplicationScope(scope))
        scope.launch { settings.setAppLockEnabled(true) }
        check(settings.current?.appLockEnabled == true) {
            "The lock setting must be in place before the app starts."
        }
        return AppLock(
            settings,
            deviceCanAuthenticate = { true },
            clock = { now },
            scope = ApplicationScope(scope),
        )
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

private const val ContentTag = "locked_content"
private const val PlaceholderTag = "placeholder"

// Robolectric refuses SDK 36 and above on a Java 17 toolchain, which is what this project builds with.
private const val ROBOLECTRIC_SDK = 35
