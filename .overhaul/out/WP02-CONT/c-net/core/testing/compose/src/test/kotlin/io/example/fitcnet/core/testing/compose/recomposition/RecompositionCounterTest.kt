package io.example.fitcnet.core.testing.compose.recomposition

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.example.fitcnet.core.testing.compose.TEST_SDK
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class RecompositionCounterTest {

    private val counter = RecompositionCounter()
    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(counter).around(compose)

    @Test
    fun `a slide read in layout recomposes only on the frame its target changes`() {
        var moved by mutableStateOf(false)
        compose.setContent { Slider(moved, readInLayout = true) }
        compose.mainClock.autoAdvance = false
        compose.waitForIdle()
        counter.reset()

        moved = true
        val frames = compose.framesUntilIdle(counter)

        assertTrue("the slide took ${frames.size} frames", frames.size > 3)
        frames.assertQuietBetweenEnds()
        assertEquals(1, counter.count("Slider"))
    }

    @Test
    fun `a slide read in composition is reported frame by frame`() {
        var moved by mutableStateOf(false)
        compose.setContent { Slider(moved, readInLayout = false) }
        compose.mainClock.autoAdvance = false
        compose.waitForIdle()
        counter.reset()

        moved = true
        val frames = compose.framesUntilIdle(counter)

        val failure = assertThrows(AssertionError::class.java) { frames.assertQuietBetweenEnds() }
        assertTrue(failure.message.orEmpty().contains("Slider"))
        assertTrue(counter.count("Slider") > 3)
    }
}

@Composable
private fun Slider(moved: Boolean, readInLayout: Boolean) {
    val offset = animateFloatAsState(if (moved) 200f else 0f, label = "offset")
    val modifier = if (readInLayout) {
        Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }
    } else {
        Modifier.padding(start = offset.value.dp)
    }
    Box(modifier.size(10.dp))
}
