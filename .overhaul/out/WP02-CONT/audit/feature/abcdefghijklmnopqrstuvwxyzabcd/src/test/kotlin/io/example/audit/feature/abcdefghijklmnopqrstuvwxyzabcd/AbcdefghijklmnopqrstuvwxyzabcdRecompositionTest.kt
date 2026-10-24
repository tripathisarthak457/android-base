package io.example.audit.feature.abcdefghijklmnopqrstuvwxyzabcd

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import io.example.audit.core.common.LoadState
import io.example.audit.core.designsystem.theme.AppTheme
import io.example.audit.core.testing.compose.TEST_SDK
import io.example.audit.core.testing.compose.recomposition.RecompositionCounter
import io.example.audit.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem
import io.example.audit.feature.abcdefghijklmnopqrstuvwxyzabcd.list.AbcdefghijklmnopqrstuvwxyzabcdListScreen
import io.example.audit.feature.abcdefghijklmnopqrstuvwxyzabcd.list.AbcdefghijklmnopqrstuvwxyzabcdListState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The list's budget: a scroll composes the rows that come into view, and nothing else. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class AbcdefghijklmnopqrstuvwxyzabcdRecompositionTest {

    private val counter = RecompositionCounter()
    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(counter).around(compose)

    @Test
    fun `scrolling leaves the rows already on screen and the screen itself alone`() {
        val items = List(ROWS) { AbcdefghijklmnopqrstuvwxyzabcdItem(id = "$it", title = "Row $it") }
        compose.setContent {
            AppTheme {
                AbcdefghijklmnopqrstuvwxyzabcdListScreen(
                    state = AbcdefghijklmnopqrstuvwxyzabcdListState(
                        loadState = LoadState.Success,
                        items = items,
                    ),
                    onEvent = {},
                )
            }
        }
        compose.waitForIdle()
        counter.reset()

        compose.onNodeWithTag("primary_list").performScrollToIndex(SCROLLED_TO)
        compose.waitForIdle()

        assertEquals(
            "the screen recomposed while its list scrolled",
            0,
            counter.count("AbcdefghijklmnopqrstuvwxyzabcdListScreen"),
        )
        // The rows that scrolled in, plus at most one the list composed ahead of need.
        val rows = counter.count("AbcdefghijklmnopqrstuvwxyzabcdRow")
        assertTrue("$rows rows composed for a scroll of $SCROLLED_TO", rows in 1..SCROLLED_TO + 2)
    }

    private companion object {
        const val ROWS = 50
        const val SCROLLED_TO = 3
    }
}
