package io.example.fit.feature.ab

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import io.example.fit.core.common.LoadState
import io.example.fit.core.designsystem.theme.AppTheme
import io.example.fit.core.testing.compose.TEST_SDK
import io.example.fit.core.testing.compose.recomposition.RecompositionCounter
import io.example.fit.data.ab.api.model.AbItem
import io.example.fit.feature.ab.list.AbListScreen
import io.example.fit.feature.ab.list.AbListState
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
class AbRecompositionTest {

    private val counter = RecompositionCounter()
    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(counter).around(compose)

    @Test
    fun `scrolling leaves the rows already on screen and the screen itself alone`() {
        val items = List(ROWS) { AbItem(id = "$it", title = "Row $it") }
        compose.setContent {
            AppTheme {
                AbListScreen(
                    state = AbListState(loadState = LoadState.Success, items = items),
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
            counter.count("AbListScreen"),
        )
        // The rows that scrolled in, plus at most one the list composed ahead of need.
        val rows = counter.count("AbRow")
        assertTrue("$rows rows composed for a scroll of $SCROLLED_TO", rows in 1..SCROLLED_TO + 2)
    }

    private companion object {
        const val ROWS = 50
        const val SCROLLED_TO = 3
    }
}
