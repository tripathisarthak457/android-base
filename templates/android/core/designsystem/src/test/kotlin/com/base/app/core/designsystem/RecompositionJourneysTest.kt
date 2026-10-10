package com.base.app.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.overlay.AppContextMenu
import com.base.app.core.designsystem.component.overlay.AppMenuItem
import com.base.app.core.designsystem.component.refresh.AppPullToRefresh
import com.base.app.core.designsystem.component.selection.AppChip
import com.base.app.core.designsystem.component.selection.AppSegmentedControl
import com.base.app.core.designsystem.component.selection.AppSwitch
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppMotionStyle
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The design system's journeys, counted in composables that ran: a press, a drag across a switch or
 * a segmented control, a pull to refresh, a fling into the end of a list and a context menu opening
 * move layers and redraw, and recompose nothing but the state that really changed. Each runs under
 * a quick and a slow motion style too: the counts must not depend on how many frames the motion
 * took, or something recomposes every frame.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
class RecompositionJourneysTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val counter = RecompositionCounter()

    private var style by mutableStateOf(AppMotionStyle.Standard)

    private fun setContent(content: @Composable () -> Unit) {
        rule.setContent {
            key(style) { AppTheme(mode = ThemeMode.Light, motionStyle = style, reduceMotion = false, content = content) }
        }
        rule.waitForIdle()
        counter.reset()
    }

    /**
     * Runs [journey] under a quick and a slow motion style and checks both ran the same composables
     * the same number of times: a count that grows with the number of frames means something
     * recomposes every frame.
     */
    private fun inEveryStyle(journey: () -> Unit) {
        val counts = listOf(AppMotionStyle.Snappy, AppMotionStyle.Calm).map {
            style = it
            rule.waitForIdle()
            counter.reset()
            journey()
            counter.executed()
        }
        assertEquals("a quick and a slow style recomposed differently", counts[0], counts[1])
    }

    private fun assertNothingRan(journey: String) =
        assertTrue("$journey recomposed ${counter.executed()}", counter.executed().isEmpty())

    @Test
    fun `holding and releasing a control recomposes nothing`() {
        setContent {
            Column {
                AppButton(text = "Save", onClick = {}, modifier = Modifier.testTag("button"))
                AppIconButton(AppIcons.Share, contentDescription = "Share", onClick = {}, modifier = Modifier.testTag("icon"))
                AppRow(title = "Row", onClick = {}, modifier = Modifier.testTag("row"))
                AppCard(onClick = {}, modifier = Modifier.testTag("card")) { AppText("Card") }
                AppChip(label = "Chip", onClick = {}, modifier = Modifier.testTag("chip"))
            }
        }
        for (tag in listOf("button", "icon", "row", "card", "chip")) {
            rule.onNodeWithTag(tag).performTouchInput { down(center) }
            rule.mainClock.advanceTimeBy(400)
            rule.onNodeWithTag(tag).performTouchInput { up() }
            rule.mainClock.advanceTimeBy(1_000)
        }
        assertNothingRan("pressing")
    }

    @Test
    fun `dragging a switch recomposes only for the value it lands on`() {
        var checked by mutableStateOf(false)
        setContent { AppSwitch(checked = checked, onCheckedChange = { checked = it }, modifier = Modifier.testTag("switch")) }
        inEveryStyle {
            checked = false
            rule.waitForIdle()
            counter.reset()
            rule.onNodeWithTag("switch").performTouchInput {
                down(centerLeft)
                repeat(10) { moveBy(Offset(width / 10f, 0f)) }
            }
            rule.mainClock.advanceTimeBy(200)
            assertNothingRan("dragging the thumb")
            rule.onNodeWithTag("switch").performTouchInput { up() }
            rule.mainClock.advanceTimeBy(1_000)
            assertTrue(checked)
            assertTrue("landing on a value recomposed ${counter.executed()}", counter.count("AppSwitch") <= 1)
        }
    }

    @Test
    fun `dragging the segmented thumb recomposes only for the segment it lands on`() {
        var selected by mutableIntStateOf(0)
        setContent {
            AppSegmentedControl(options = listOf("Day", "Week", "Month"), selected = selected, onSelect = { selected = it }, modifier = Modifier.testTag("segments"))
        }
        rule.onNodeWithTag("segments").performTouchInput {
            down(Offset(width / 6f, centerY))
            repeat(12) { moveBy(Offset(width / 18f, 0f)) }
        }
        rule.mainClock.advanceTimeBy(200)
        assertNothingRan("dragging the thumb")
        rule.onNodeWithTag("segments").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(2, selected)
        assertTrue("landing recomposed ${counter.executed()}", counter.count("AppSegmentedControl") <= 1)
    }

    @Test
    fun `a pull to refresh recomposes only for the refresh starting and ending`() {
        var refreshing by mutableStateOf(false)
        setContent {
            AppPullToRefresh(isRefreshing = refreshing, onRefresh = { refreshing = true }, modifier = Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize().testTag("list")) {
                    items((1..40).toList(), key = { it }) { JourneyRow() }
                }
            }
        }
        inEveryStyle {
            rule.onNodeWithTag("list").performTouchInput {
                down(Offset(centerX, top + 10f))
                repeat(20) { moveBy(Offset(0f, 30f)) }
            }
            rule.mainClock.advanceTimeBy(300)
            rule.onNodeWithTag("list").performTouchInput { up() }
            rule.mainClock.advanceTimeBy(1_000)
            refreshing = false
            rule.mainClock.advanceTimeBy(2_000)
            assertEquals("the rows recomposed: ${counter.executed()}", 0, counter.count("JourneyRow"))
            assertTrue("a pull recomposed ${counter.executed()}", counter.count("AppPullToRefresh") <= 2)
        }
    }

    @Test
    fun `a fling into the top of a list bounces without recomposing its rows`() {
        setContent {
            LazyColumn(Modifier.fillMaxSize().testTag("list")) {
                items((1..60).toList(), key = { it }) { AppRow(title = "Row $it") }
            }
        }
        rule.onNodeWithTag("list").performTouchInput { swipeDown(startY = top + 10f, endY = bottom - 10f, durationMillis = 120) }
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals("rows recomposed while the edge bounced: ${counter.executed()}", 0, counter.count("AppRow"))
    }

    @Test
    fun `opening a context menu recomposes nothing on the page`() {
        setContent {
            LazyColumn(Modifier.fillMaxSize()) {
                items((1..20).toList(), key = { it }) { row ->
                    AppContextMenu(menu = { AppMenuItem(text = "Share", onClick = {}) }) {
                        AppRow(title = "Row $row", onClick = {}, modifier = Modifier.testTag("row$row"))
                    }
                }
            }
        }
        rule.onNodeWithTag("row3").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(800)
        rule.onNodeWithTag("row3", useUnmergedTree = true).performTouchInput { up() }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithText("Share").assertExists()
        assertEquals("rows recomposed as the menu opened: ${counter.executed()}", 0, counter.count("AppRow"))
    }
}

@Composable
private fun JourneyRow() {
    Box(Modifier.height(56.dp))
}
