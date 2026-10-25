package com.base.app.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.base.app.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Serializable
private data object Inbox : AppNavKey

@Serializable
private data class Conversation(val id: Int, val title: String) : AppNavKey

/**
 * A destination is registered once, with `entry<K>`, and that registration is also how its key is
 * written into the saved back stack: there is no second list for a key to be missing from.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class NavKeyRestoreTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a key registered only with entry survives save and restore`() {
        val graph = navGraph {
            entry<Inbox> {}
            entry<Conversation> {}
        }
        val serialization = NavKeySerialization(setOf(graph))
        val restoration = StateRestorationTester(rule)
        var stack: AppBackStack? = null
        restoration.setContent {
            stack = rememberAppBackStack(startKey = Inbox, serialization = serialization)
        }
        rule.runOnIdle { stack!!.apply(NavCommand.Navigate(Conversation(7, "Launch"))) }
        val before = stack

        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        assertNotSame("the stack was rebuilt from what was saved", before, stack)
        assertEquals(listOf(Inbox, Conversation(7, "Launch")), stack!!.keys)
    }

    @Test
    fun `every graph's keys are written with the one serialization`() {
        val serialization = NavKeySerialization(
            setOf(
                navGraph { entry<Inbox> {} },
                navGraph { entry<Conversation> {} },
            ),
        )

        val keys = listOf(Inbox, Conversation(1, "a"))

        assertEquals(keys, serialization.decodeStack(serialization.encodeStack(keys)!!))
    }
}
