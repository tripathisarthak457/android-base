package com.example.wp04.everything.core.designsystem.component.feedback

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.wp04.everything.core.designsystem.TestSdk
import com.example.wp04.everything.core.designsystem.component.container.AppCard
import com.example.wp04.everything.core.designsystem.component.container.AppScaffold
import com.example.wp04.everything.core.designsystem.component.navigation.AppLargeTitleTopBar
import com.example.wp04.everything.core.designsystem.component.navigation.scrolledFromTop
import com.example.wp04.everything.core.designsystem.component.text.AppText
import com.example.wp04.everything.core.designsystem.foundation.ChromeInset
import com.example.wp04.everything.core.designsystem.foundation.LocalBottomChrome
import com.example.wp04.everything.core.designsystem.foundation.LocalTopChrome
import com.example.wp04.everything.core.designsystem.foundation.chromeContentPadding
import com.example.wp04.everything.core.designsystem.foundation.draw.LocalShimmerClock
import com.example.wp04.everything.core.designsystem.foundation.draw.ShimmerClock
import com.example.wp04.everything.core.designsystem.theme.AppTheme
import com.example.wp04.everything.core.designsystem.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * A loading screen as an app builds it, and the same screen loaded, under a large title and above
 * 80 dp of bottom chrome, in both themes and at font scales 1 and 1.5: the placeholder rows start
 * below the title, end above the chrome, and are exactly the rows that replace them. The frames,
 * caught mid-sweep, are written to `build/outputs/skeleton/` for a person to look at.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkeletonScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private var fontScale by mutableFloatStateOf(1f)
    private var dark by mutableStateOf(false)
    private var loaded by mutableStateOf(false)

    @Test
    fun `placeholder rows sit between the chrome and are the rows that replace them`() {
        var clock: ShimmerClock? = null
        var topChrome = Dp.Unspecified
        val list = LazyListState()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Environment {
                clock = LocalShimmerClock.current
                LoadingScreen(list, loaded) { topChrome = it }
            }
        }
        for (scale in listOf(1f, 1.5f)) {
            for (theme in listOf(false, true)) {
                fontScale = scale
                dark = theme
                loaded = false
                val name = "${if (theme) "dark" else "light"}-font${(scale * 100).toInt()}"
                settleMidSweep { clock }
                save(name)
                val placeholders = rowBounds()
                val density = rule.density
                val gutter = with(density) { Gutter.toPx() }

                val first = placeholders.first()
                val belowTitle = with(density) { topChrome.toPx() } + gutter
                assertTrue("$name: the first row starts at ${first.top}, under the title", first.top >= belowTitle - 1)
                val layout = list.layoutInfo
                val bottomChrome = with(density) { BottomChrome.toPx() }
                assertTrue(
                    "$name: the list ends ${layout.afterContentPadding} px above the bottom",
                    layout.afterContentPadding >= bottomChrome + gutter - 1,
                )

                loaded = true
                settle()
                save("$name-loaded")
                assertEquals("$name: the rows moved when the content arrived", placeholders, rowBounds())
            }
        }
    }

    @Test
    fun `a state view keeps its title and action between the chrome at a large font`() {
        rule.mainClock.autoAdvance = false
        fontScale = 1.5f
        var topChrome = Dp.Unspecified
        rule.setContent {
            Environment {
                AppScaffold(
                    topBar = { AppLargeTitleTopBar(title = "Inbox", subtitle = "All mail", scrolled = { 0 }) },
                ) {
                    topChrome = LocalTopChrome.current.value
                    AppStateView(
                        kind = StateKind.Offline,
                        title = "No connection",
                        message = "Showing what was saved last time. Pull to try again once you are back online.",
                        actionLabel = "Try again",
                        onAction = {},
                    )
                }
            }
        }
        settle()
        save("state-font150")
        val root = rule.onRoot().getBoundsInRoot()
        val title = rule.onNodeWithText("No connection").getBoundsInRoot()
        val action = rule.onNodeWithText("Try again", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("the title at ${title.top} is under the top bar ending at $topChrome", title.top >= topChrome)
        assertTrue(
            "the action at ${action.bottom} is under the bottom chrome",
            action.bottom <= root.bottom - BottomChrome,
        )
    }

    @Composable
    private fun Environment(content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            AppTheme(mode = if (dark) ThemeMode.Dark else ThemeMode.Light, reduceMotion = false) {
                val chrome = remember { ChromeInset().apply { value = BottomChrome } }
                CompositionLocalProvider(LocalBottomChrome provides chrome, content = content)
            }
        }
    }

    /**
     * Lets a state change reach the composition and lays out the frames it causes. With the clock
     * advanced by hand, nothing else applies a change made while no frame was asked for.
     */
    private fun settle() {
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
    }

    /** Lays the screen out, then runs the sweep until its band is half way across. */
    private fun settleMidSweep(clock: () -> ShimmerClock?) {
        settle()
        var frames = 0
        while (frames++ < MaxFrames) {
            rule.mainClock.advanceTimeByFrame()
            val progress = clock()?.progress ?: continue
            if (progress in 0.45f..0.55f) return
        }
        error("the sweep never reached its middle")
    }

    private fun rowBounds(): List<Rect> = (0 until VisibleRows).map { index ->
        val bounds = rule.onNodeWithTag("row$index", useUnmergedTree = true).getBoundsInRoot()
        with(rule.density) { Rect(bounds.left.toPx(), bounds.top.toPx(), bounds.right.toPx(), bounds.bottom.toPx()) }
    }

    private fun save(name: String) {
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        val file = File("build/outputs/skeleton/$name.png").apply { parentFile?.mkdirs() }
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        val BottomChrome = 80.dp
        val Gutter = 16.dp
        const val VisibleRows = 4
        const val MaxFrames = 400
    }
}

/** A list screen while it loads: the rows it will show, redacted, under a large title. */
@Composable
private fun LoadingScreen(list: LazyListState, loaded: Boolean, onTopChrome: (Dp) -> Unit) {
    AppScaffold(
        topBar = {
            AppLargeTitleTopBar(title = "Inbox", subtitle = "All mail", scrolled = { list.scrolledFromTop() })
        },
    ) {
        onTopChrome(LocalTopChrome.current.value)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = list,
            contentPadding = chromeContentPadding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
            userScrollEnabled = loaded,
        ) {
            itemsIndexed(Messages) { index, message ->
                Box(Modifier.testTag("row$index")) {
                    AppRedacted(active = !loaded) { MessageRow(message) }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(message: Pair<String, String>) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            AppAvatar(name = message.first)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                AppText(
                    message.first,
                    style = AppTheme.typography.headline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AppText(
                    message.second,
                    style = AppTheme.typography.subheadline,
                    color = AppTheme.colors.contentSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val Messages = List(12) { index ->
    "Sender ${index + 1}" to "A message preview long enough to wrap onto a second line on a phone, cut there."
}
