package com.base.app.core.image

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.base.app.core.testing.compose.TEST_SDK
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** Prefetch fetches bytes ahead in the direction of travel, two at a time, and nothing it should not. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK], qualifiers = "w360dp-h800dp")
class PrefetchImagesTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val started = mutableListOf<ImageRequest>()
    private val unfinished = mutableMapOf<Int, CompletableDeferred<Unit>>()
    private lateinit var loader: ImageLoader
    private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())

    @Before
    fun installLoader() {
        val network = Interceptor { chain ->
            val gate = CompletableDeferred<Unit>()
            started += chain.request
            unfinished[index(chain.request)] = gate
            try {
                gate.await()
            } finally {
                unfinished.remove(index(chain.request))
            }
            ErrorResult(image = null, request = chain.request, throwable = IllegalStateException("not fetched in tests"))
        }
        loader = ImageLoader.Builder(context).components { add(network) }.build()
        SingletonImageLoader.setUnsafe(loader)
    }

    @After
    fun tearDown() {
        scope.cancel()
        SingletonImageLoader.reset()
    }

    private fun index(request: ImageRequest) = request.data.toString().substringAfterLast('/').toInt()

    private fun startedIndices() = started.map(::index)

    private fun prefetcher() = Prefetcher(scope, loader, context) { "https://img.test/$it" }

    private fun idle() = ShadowLooper.idleMainLooper()

    private fun finishOne() {
        unfinished.entries.minBy { it.key }.value.complete(Unit)
        idle()
    }

    @Test
    fun `it fetches bytes two items past the last one on screen`() {
        prefetcher().onWindow(ListWindow(first = 10, last = 19, count = 100))
        idle()

        assertEquals(listOf(21, 22), startedIndices())
        started.forEach { request ->
            assertEquals(CachePolicy.DISABLED, request.memoryCachePolicy)
            assertTrue(request.decoderFactory === BytesOnly)
        }
    }

    @Test
    fun `never more than two at once`() {
        prefetcher().onWindow(ListWindow(first = 10, last = 19, count = 100))
        idle()
        repeat(5) {
            assertTrue("${unfinished.keys}", unfinished.size <= Prefetcher.IN_FLIGHT)
            finishOne()
        }

        assertEquals(listOf(21, 22, 23, 24, 25, 26, 27), startedIndices())
    }

    @Test
    fun `turning back drops what waits, and fetches behind`() {
        val prefetcher = prefetcher()
        prefetcher.onWindow(ListWindow(first = 50, last = 59, count = 100))
        idle()
        prefetcher.onWindow(ListWindow(first = 49, last = 58, count = 100))
        finishOne()
        finishOne()

        assertEquals(listOf(61, 62, 47, 46), startedIndices())
    }

    @Test
    fun `nothing on screen or composed next is fetched, and nothing twice`() {
        val prefetcher = prefetcher()
        prefetcher.onWindow(ListWindow(first = 0, last = 9, count = 100))
        idle()
        prefetcher.onWindow(ListWindow(first = 1, last = 10, count = 100))
        repeat(40) { if (unfinished.isNotEmpty()) finishOne() }

        val indices = startedIndices()
        assertTrue(indices.none { it in 0..10 })
        assertEquals(indices.distinct(), indices)
        assertTrue("reaches about three screens: $indices", indices.max() in 30..40)
    }

    @Test
    fun `a list, a grid and a pager each fetch ahead of what shows`() {
        var which by mutableStateOf(0)
        val list = LazyListState()
        val grid = LazyGridState()
        val pager = PagerState { 100 }
        rule.setContent {
            when (which) {
                0 -> {
                    LazyColumn(state = list) { items(100, key = { it }, contentType = { "row" }) { Box(Modifier.height(100.dp)) } }
                    PrefetchImages(list) { "https://img.test/$it" }
                }
                1 -> {
                    LazyVerticalGrid(GridCells.Fixed(2), state = grid) {
                        items(100, key = { it }, contentType = { "cell" }) { Box(Modifier.height(100.dp)) }
                    }
                    PrefetchImages(grid) { "https://img.test/$it" }
                }
                else -> {
                    VerticalPager(state = pager, key = { it }) { Box(Modifier.fillMaxSize()) }
                    PrefetchImages(pager) { "https://img.test/$it" }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(list.layoutInfo.visibleItemsInfo.last().index + Prefetcher.GAP, startedIndices().first())

        val forGrid = startedBy { which = 1 }
        assertEquals(grid.layoutInfo.visibleItemsInfo.last().index + Prefetcher.GAP, forGrid.first())

        val forPager = startedBy { which = 2 }
        assertEquals(pager.layoutInfo.visiblePagesInfo.last().index + Prefetcher.GAP, forPager.first())
    }

    /** What starts after [change]: the previous list's requests stay blocked, and leave with it. */
    private fun startedBy(change: () -> Unit): List<Int> {
        val before = started.size
        change()
        rule.waitForIdle()
        return startedIndices().drop(before)
    }

    @Test
    fun `leaving the screen cancels what waits`() {
        var shown by mutableStateOf(true)
        val list = LazyListState()
        rule.setContent {
            LazyColumn(state = list) { items(100, key = { it }, contentType = { "row" }) { Box(Modifier.height(100.dp)) } }
            if (shown) PrefetchImages(list) { "https://img.test/$it" }
        }
        rule.waitForIdle()
        val before = started.size

        shown = false
        rule.waitForIdle()
        unfinished.values.toList().forEach { it.complete(Unit) }
        rule.waitForIdle()

        assertEquals(before, started.size)
    }
}
