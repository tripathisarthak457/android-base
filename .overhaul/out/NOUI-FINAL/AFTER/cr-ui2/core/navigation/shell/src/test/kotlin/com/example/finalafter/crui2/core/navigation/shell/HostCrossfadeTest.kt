package com.example.finalafter.crui2.core.navigation.shell

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.finalafter.crui2.core.common.navigation.AppNavKey
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import com.example.finalafter.crui2.core.navigation.AppBackStack
import com.example.finalafter.crui2.core.navigation.AppNavigator
import com.example.finalafter.crui2.core.navigation.rememberAppBackStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The one whole-screen crossfade: while both hosts are composed, only the arriving one may act. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class HostCrossfadeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val navigator = AppNavigator()
    private val stacks = arrayOfNulls<AppBackStack>(2)
    private var target by mutableIntStateOf(0)

    /** Host 0 starts at the feed, host 1 at the profile, whose screen runs [onBuilt] as it is built. */
    private fun showHosts(onBuilt: () -> Unit = {}) {
        val registry = testRegistry { key ->
            if (key == ProfileTab) remember { CreatedWithScreen(onBuilt) }
            Screen(key)
        }
        rule.setContent {
            AppTheme {
                HostCrossfade(target = target) { host, receivesCommands ->
                    val start: AppNavKey = if (host == 0) FeedTab else ProfileTab
                    val stack = rememberAppBackStack(startKey = start, serialization = testSerialization)
                    stacks[host] = stack
                    AppNavigationHost(stack, registry, navigator, receivesCommands = receivesCommands)
                }
            }
        }
        rule.waitForIdle()
    }

    /**
     * Starts the crossfade to host 1 with the clock paused. A write outside composition reaches the
     * recomposer only once snapshot changes are applied, which a paused frame does not do and
     * waiting for idle does.
     */
    private fun startCrossfade() {
        rule.runOnIdle { target = 1 }
        rule.waitForIdle()
    }

    @Test
    fun `a command sent while the arriving host is built goes to it, not to the one leaving`() {
        // As a ViewModel created with the arriving host's first screen would send it.
        showHosts(onBuilt = { navigator.navigate(Post(2)) })
        rule.mainClock.autoAdvance = false

        startCrossfade()
        rule.mainClock.advanceTimeBy(CROSSFADE_AND_SLIDE_MS)

        assertEquals(listOf(FeedTab), stacks[0]!!.keys)
        assertEquals(listOf(ProfileTab, Post(2)), stacks[1]!!.keys)
    }

    @Test
    fun `Back pressed during the crossfade pops neither host`() {
        showHosts()
        rule.runOnIdle { navigator.navigate(Post(1)) }
        rule.waitForIdle()
        assertEquals(listOf(FeedTab, Post(1)), stacks[0]!!.keys)
        rule.mainClock.autoAdvance = false

        startCrossfade()
        repeat(MID_CROSSFADE_FRAMES) { rule.mainClock.advanceTimeByFrame() }
        rule.runOnIdle { assertNotNull("the arriving host is composed", stacks[1]) }
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.mainClock.advanceTimeBy(CROSSFADE_AND_SLIDE_MS)

        assertEquals(listOf(FeedTab, Post(1)), stacks[0]!!.keys)
        assertEquals(listOf(ProfileTab), stacks[1]!!.keys)
    }
}

/** Runs [onCreated] as it is created, as a ViewModel created with its screen runs its `init`. */
private class CreatedWithScreen(onCreated: () -> Unit) {
    init {
        onCreated()
    }
}

/** Both hosts composed, well short of the 200 ms fade's end. */
private const val MID_CROSSFADE_FRAMES = 4
private const val CROSSFADE_AND_SLIDE_MS = 2_000L
