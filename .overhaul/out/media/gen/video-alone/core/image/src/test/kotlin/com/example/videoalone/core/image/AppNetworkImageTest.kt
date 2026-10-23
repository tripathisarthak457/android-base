package com.example.videoalone.core.image

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transitionFactory
import coil3.size.Size
import coil3.transition.CrossfadeTransition
import coil3.transition.Transition
import com.example.videoalone.core.designsystem.component.feedback.AppRedacted
import com.example.videoalone.core.designsystem.theme.AppTheme
import com.example.videoalone.core.testing.compose.TEST_SDK
import com.example.videoalone.core.testing.compose.recomposition.RecompositionCounter
import com.example.videoalone.core.testing.compose.recomposition.framesUntilIdle
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A remote image costs a list one composition per row: a finished load redraws that image and
 * nothing else, the request it makes is the size of its slot, and a fade happens only where asked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK], qualifiers = "w360dp-h800dp-xxhdpi")
class AppNetworkImageTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val counter = RecompositionCounter()

    private class Seen(val request: ImageRequest, val size: Size)

    private val seen = CopyOnWriteArrayList<Seen>()
    private val loads = CompletableDeferred<Unit>()

    @Before
    fun installLoader() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val image = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).asImage()
        val network = Interceptor { chain ->
            seen += Seen(chain.request, chain.size)
            loads.await()
            SuccessResult(image = image, request = chain.request, dataSource = DataSource.NETWORK)
        }
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(context).components { add(network) }.build())
    }

    @After
    fun resetLoader() = SingletonImageLoader.reset()

    @Test
    fun `a load that finishes recomposes nothing and lays no row out again`() {
        var measures = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme {
                LazyColumn {
                    items(count = 20, key = { it }, contentType = { "row" }) { index ->
                        Box(
                            Modifier.layout { measurable, constraints ->
                                measures++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            },
                        ) {
                            AppNetworkImage(url = "https://img.test/$index", contentDescription = null, modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }
        }
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        assertTrue("no request started: ${seen.size}", seen.isNotEmpty())
        counter.reset()
        val measuredBefore = measures

        loads.complete(Unit)
        rule.framesUntilIdle(counter)

        assertEquals(0, counter.count("AppNetworkImage"))
        assertEquals(measuredBefore, measures)
    }

    @Test
    fun `an image fades in only when it asks to`() {
        rule.setContent {
            AppTheme {
                Column {
                    AppNetworkImage(url = "https://img.test/plain", contentDescription = null, modifier = Modifier.size(48.dp))
                    AppNetworkImage(
                        url = "https://img.test/fades",
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        fadeIn = true,
                    )
                }
                AppTheme(reduceMotion = true) {
                    AppNetworkImage(
                        url = "https://img.test/still",
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        fadeIn = true,
                    )
                }
            }
        }
        rule.waitForIdle()

        val fades = seen.associate { it.request.data.toString().substringAfterLast('/') to it.request.transitionFactory }
        assertEquals(Transition.Factory.NONE, fades["plain"])
        val crossfade = fades["fades"] as CrossfadeTransition.Factory
        assertEquals(200, crossfade.durationMillis)
        assertEquals(Transition.Factory.NONE, fades["still"])
    }

    @Test
    fun `the same url keeps one request across recompositions`() {
        var unrelated by mutableIntStateOf(0)
        rule.setContent {
            AppTheme {
                Column {
                    Box(Modifier.width((unrelated + 1).dp))
                    AppNetworkImage(url = "https://img.test/one", contentDescription = null, modifier = Modifier.size(48.dp))
                }
            }
        }
        rule.waitForIdle()
        repeat(3) {
            unrelated++
            rule.waitForIdle()
        }

        assertEquals(1, seen.size)
    }

    @Test
    fun `a redacted image loads nothing`() {
        rule.setContent {
            AppTheme {
                AppRedacted(active = true) {
                    AppNetworkImage(url = "https://img.test/hidden", contentDescription = null, modifier = Modifier.size(48.dp))
                }
            }
        }
        rule.waitForIdle()

        assertEquals(0, seen.size)
    }

    @Test
    fun `an image asks for its slot's pixels`() {
        rule.setContent {
            AppTheme {
                Column(Modifier.width(360.dp)) {
                    AppNetworkImage(url = "https://img.test/fixed", contentDescription = null, modifier = Modifier.size(100.dp))
                    AppNetworkImage(
                        url = "https://img.test/ratio",
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.8f),
                    )
                }
            }
        }
        rule.waitForIdle()

        val sizes = seen.associate { it.request.data.toString().substringAfterLast('/') to it.size }
        assertEquals(Size(300, 300), sizes["fixed"])
        assertEquals(Size(1080, 1350), sizes["ratio"])
    }
}
