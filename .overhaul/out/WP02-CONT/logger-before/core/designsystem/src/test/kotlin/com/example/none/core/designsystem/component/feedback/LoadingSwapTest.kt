package com.example.none.core.designsystem.component.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.none.core.designsystem.TestSdk
import com.example.none.core.designsystem.animation.appAnimateItem
import com.example.none.core.designsystem.component.container.AppCard
import com.example.none.core.designsystem.component.text.AppText
import com.example.none.core.designsystem.setThemed
import com.example.none.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A loading list swapped for the loaded list in one frame, as the screens do it, each list on a
 * state of its own, with motion on: the loaded rows, which animate when the list changes, are
 * whole on the first frame after the swap, and nothing moves or fades after it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoadingSwapTest {

    @get:Rule
    val rule = createComposeRule()

    private var loaded by mutableStateOf(false)

    @Test
    fun `the loaded rows are whole on the first frame after the swap`() {
        rule.mainClock.autoAdvance = false
        rule.setThemed {
            val list = rememberLazyListState()
            Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
                if (loaded) LoadedList(list) else LoadingList()
            }
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(SettleMillis)

        loaded = true
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        val first = rule.onRoot().captureToImage().toPixelMap()
        rule.mainClock.advanceTimeBy(SettleMillis)
        val settled = rule.onRoot().captureToImage().toPixelMap()

        assertEquals("pixels that changed after the first frame", 0, first.countDifferences(settled))
    }

    private fun PixelMap.countDifferences(other: PixelMap): Int {
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (this[x, y] != other[x, y]) count++
            }
        }
        return count
    }

    private companion object {
        /** Longer than any row animation. */
        const val SettleMillis = 1_000L
    }
}

@Composable
private fun LoadedList(state: LazyListState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
    ) {
        items(Messages, key = Message::id) { message ->
            MessageRow(message, modifier = appAnimateItem())
        }
    }
}

@Composable
private fun LoadingList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
        userScrollEnabled = false,
    ) {
        items(Messages.size) {
            AppRedacted(active = true) { MessageRow(Placeholder) }
        }
    }
}

@Composable
private fun MessageRow(message: Message, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        AppText(message.title, style = AppTheme.typography.headline)
    }
}

private class Message(val id: Int, val title: String)

private val Messages = List(8) { Message(it, "Message ${it + 1}") }

private val Placeholder = Message(-1, "Placeholder title")
