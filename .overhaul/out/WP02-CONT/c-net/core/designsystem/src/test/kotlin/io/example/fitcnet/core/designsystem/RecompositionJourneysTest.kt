package io.example.fitcnet.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import io.example.fitcnet.core.designsystem.component.button.AppButton
import io.example.fitcnet.core.designsystem.component.button.AppIconButton
import io.example.fitcnet.core.designsystem.component.container.AppCard
import io.example.fitcnet.core.designsystem.component.container.AppScaffold
import io.example.fitcnet.core.designsystem.component.feedback.AppAvatar
import io.example.fitcnet.core.designsystem.component.feedback.AppRedacted
import io.example.fitcnet.core.designsystem.component.feedback.AppSkeleton
import io.example.fitcnet.core.designsystem.component.feedback.AppSkeletonCircle
import io.example.fitcnet.core.designsystem.component.list.AppRow
import io.example.fitcnet.core.designsystem.component.navigation.AppLargeTitleTopBar
import io.example.fitcnet.core.designsystem.component.navigation.scrolledFromTop
import io.example.fitcnet.core.designsystem.component.overlay.AppContextMenu
import io.example.fitcnet.core.designsystem.component.overlay.AppMenuItem
import io.example.fitcnet.core.designsystem.component.refresh.AppPullToRefresh
import io.example.fitcnet.core.designsystem.component.selection.AppChip
import io.example.fitcnet.core.designsystem.component.selection.AppSegmentedControl
import io.example.fitcnet.core.designsystem.component.selection.AppSwitch
import io.example.fitcnet.core.designsystem.component.sheet.AppSheet
import io.example.fitcnet.core.designsystem.component.sheet.AppSheetState
import io.example.fitcnet.core.designsystem.component.sheet.SheetDetent
import io.example.fitcnet.core.designsystem.component.text.AppText
import io.example.fitcnet.core.designsystem.foundation.draw.LocalShimmerClock
import io.example.fitcnet.core.designsystem.foundation.draw.ShimmerClock
import io.example.fitcnet.core.designsystem.foundation.layout.LocalTabVisible
import io.example.fitcnet.core.designsystem.foundation.layout.TabVisibility
import io.example.fitcnet.core.designsystem.foundation.layout.chromeContentPadding
import io.example.fitcnet.core.designsystem.icon.AppIcons
import io.example.fitcnet.core.designsystem.theme.AppMotionStyle
import io.example.fitcnet.core.designsystem.theme.AppTheme
import io.example.fitcnet.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The design system's journeys, counted in composables that ran: a press, a drag across a switch or
 * a segmented control, a sheet dragged between detents, a pull to refresh, a fling into the end of a
 * list, a context menu opening and a skeleton list sweeping move layers and redraw, and recompose
 * nothing but the state that really changed. Each runs under a quick and a slow motion style too: the
 * counts must not depend on how many frames the motion took, or something recomposes every frame.
 * The optional groups count theirs beside their own tests (`WheelPickerRecompositionTest`), so a
 * project without them still compiles this one.
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
    fun `dragging a sheet between its detents recomposes nothing`() {
        val state = AppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large))
        setContent {
            AppSheet(visible = true, onDismissRequest = {}, state = state) { JourneyRow() }
        }
        rule.mainClock.advanceTimeBy(2_000)
        counter.reset()
        rule.onNodeWithTag("sheet_grabber").performTouchInput {
            down(center)
            repeat(30) { moveBy(Offset(0f, -20f)) }
            up()
        }
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals(SheetDetent.Large, state.currentDetent)
        assertNothingRan("dragging the sheet")
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

    @Test
    fun `a skeleton list sweeping recomposes nothing, and hiding it recomposes only the provider`() {
        var clock: ShimmerClock? = null
        var visible by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            key(style) {
                AppTheme(mode = ThemeMode.Light, motionStyle = style, reduceMotion = false) {
                    clock = LocalShimmerClock.current
                    val visibility = remember { TabVisibility { visible } }
                    CompositionLocalProvider(LocalTabVisible provides visibility) { SkeletonJourneyScreen() }
                }
            }
        }
        for (motion in listOf(AppMotionStyle.Snappy, AppMotionStyle.Calm)) {
            style = motion
            settle()
            counter.reset()
            assertTrue("the band never moved under $motion", sweptWithin(SkeletonJourneyMillis) { clock })
            assertNothingRan("a sweep under $motion")
        }

        visible = false
        settle()
        assertFalse("a hidden list kept the band moving", sweptWithin(SkeletonJourneyMillis) { clock })
        visible = true
        settle()
        assertTrue("the band did not come back with the list", sweptWithin(SkeletonJourneyMillis) { clock })
        val placeholders = listOf("AppRedacted", "AppText", "AppCard", "AppAvatar", "SkeletonJourneyRow", "AppSkeleton")
        for (placeholder in placeholders) {
            val ran = counter.count(placeholder)
            assertEquals("$placeholder recomposed as the list hid and showed: ${counter.executed()}", 0, ran)
        }
    }

    /**
     * Lets a state change reach the composition and lays out the frames it causes. With the clock
     * advanced by hand, nothing else applies a change made while no frame was asked for.
     */
    private fun settle() {
        rule.waitForIdle()
        repeat(3) { rule.mainClock.advanceTimeByFrame() }
    }

    /** Whether the shimmer's band showed on any frame of the next [millis]. */
    private fun sweptWithin(millis: Int, clock: () -> ShimmerClock?): Boolean {
        var swept = false
        repeat(millis / FrameMillis) {
            rule.mainClock.advanceTimeByFrame()
            if (clock()?.progress?.isNaN() == false) swept = true
        }
        return swept
    }

    private companion object {
        const val FrameMillis = 16
        const val SkeletonJourneyMillis = 4_500
    }
}

@Composable
private fun JourneyRow() {
    Box(Modifier.height(56.dp))
}

/** A screen while its list loads: redacted rows under a large title, a free block and a circle. */
@Composable
private fun SkeletonJourneyScreen() {
    val list = rememberLazyListState()
    AppScaffold(topBar = { AppLargeTitleTopBar(title = "Inbox", scrolled = { list.scrolledFromTop() }) }) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = list,
            contentPadding = chromeContentPadding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false,
        ) {
            items(8) { AppRedacted(active = true) { SkeletonJourneyRow() } }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppSkeletonCircle()
                    AppSkeleton(Modifier.size(200.dp, 40.dp))
                }
            }
        }
    }
}

@Composable
private fun SkeletonJourneyRow() {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppAvatar(name = "Ada Lovelace")
            Column {
                AppText("Ada Lovelace", style = AppTheme.typography.headline)
                AppText("Wrote the first program, a century before the machine.", maxLines = 2)
            }
        }
    }
}
