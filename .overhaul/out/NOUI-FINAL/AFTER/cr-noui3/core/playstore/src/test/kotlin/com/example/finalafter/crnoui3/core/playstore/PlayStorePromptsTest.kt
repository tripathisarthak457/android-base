package com.example.finalafter.crnoui3.core.playstore

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.finalafter.crnoui3.core.common.coroutines.ApplicationScope
import com.example.finalafter.crnoui3.core.common.time.AppClock
import com.example.finalafter.crnoui3.core.datastore.FakePreferencesDataStore
import com.example.finalafter.crnoui3.core.designsystem.theme.AppTheme
import com.example.finalafter.crnoui3.core.playstore.review.ReviewPrompt
import com.example.finalafter.crnoui3.core.playstore.update.AppUpdates
import com.example.finalafter.crnoui3.core.testing.testDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Asking Play loads its classes on the main thread, and the update check binds Play's service and
 * wakes the Play Store process: none of it may run in the frame that first shows the app, or while
 * the activity is not resumed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class PlayStorePromptsTest {

    @get:Rule
    val compose = createComposeRule()

    // Unconfined, so a check that starts runs on the test thread at once and is counted at once.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val dispatchers = testDispatchers(Dispatchers.Unconfined)
    private val updateStore = FakePreferencesDataStore()
    private val reviewStore = FakePreferencesDataStore()
    private val lifecycle = TestLifecycle()

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `nothing asks Play before the first frame is on screen`() {
        show()

        assertEquals("The update check started in the first frame.", 0, updateStore.reads)
        assertEquals("The launch was counted in the first frame.", 0, reviewStore.writes)

        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()

        assertEquals(1, updateStore.reads)
        assertEquals(1, reviewStore.writes)
    }

    @Test
    fun `nothing asks Play until the activity is resumed`() {
        lifecycle.registry.currentState = Lifecycle.State.STARTED
        show()

        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertEquals("The update check started before the activity resumed.", 0, updateStore.reads)
        assertEquals("The launch was counted before the activity resumed.", 0, reviewStore.writes)

        compose.runOnIdle { lifecycle.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitForIdle()

        assertEquals(1, updateStore.reads)
        assertEquals(1, reviewStore.writes)
    }

    private fun show() {
        val context = RuntimeEnvironment.getApplication()
        val updates = AppUpdates(context, updateStore, ApplicationScope(scope), dispatchers)
        val review = ReviewPrompt(context, reviewStore, AppClock.System)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycle) {
                AppTheme { PlayStorePrompts(updates, review) }
            }
        }
    }

    private class TestLifecycle : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply {
            currentState = Lifecycle.State.RESUMED
        }
        override val lifecycle: Lifecycle get() = registry
    }
}

// Robolectric refuses SDK 36 and above on the Java 17 toolchain this project builds with.
private const val ROBOLECTRIC_SDK = 35
