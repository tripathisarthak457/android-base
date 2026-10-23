package com.base.app.core.image

import android.graphics.Bitmap
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.compose.LocalPlatformContext
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.Options
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Fetches the images of the items ahead of a scrolling list into the disk cache, so a fling meets
 * images that only need decoding. Bytes only: nothing is decoded or held in memory until a row asks
 * for its image at its own size. [url] gives an item's image, or null for an item without one.
 *
 * It starts two lines past the last one on screen, because the list's own prefetch already composes
 * the next line and starts its decodes at the right size, and reaches about three screens ahead in
 * the direction of travel. At most two requests run at once, so the images on screen never queue
 * behind it in the HTTP client. An item that comes within a line of the screen is left to its row:
 * waiting, it is dropped; fetching, it is cancelled, or both would download it. Turning back drops
 * what waits, and leaving the screen cancels everything.
 */
@Composable
fun PrefetchImages(state: LazyListState, url: (index: Int) -> String?) =
    PrefetchImages(state, url, first = { state.firstVisibleItemIndex }) {
        val info = state.layoutInfo
        info.visibleItemsInfo.lastOrNull()?.let { last -> ListWindow(state.firstVisibleItemIndex, last.index, info.totalItemsCount) }
    }

/** [PrefetchImages] for a grid, counted in rows: a grid composes its whole next row itself. */
@Composable
fun PrefetchImages(state: LazyGridState, url: (index: Int) -> String?) =
    PrefetchImages(state, url, first = { state.firstVisibleItemIndex }) {
        val info = state.layoutInfo
        info.visibleItemsInfo.lastOrNull()?.let { last ->
            ListWindow(state.firstVisibleItemIndex, last.index, info.totalItemsCount, span = maxOf(info.maxSpan, 1))
        }
    }

/** [PrefetchImages] for a pager, such as a video feed's posters. */
@Composable
fun PrefetchImages(state: PagerState, url: (page: Int) -> String?) =
    PrefetchImages(state, url, first = { state.currentPage }) {
        state.layoutInfo.visiblePagesInfo.let { pages ->
            if (pages.isEmpty()) null else ListWindow(pages.first().index, pages.last().index, state.pageCount)
        }
    }

@Composable
private fun PrefetchImages(key: Any, url: (Int) -> String?, first: () -> Int, window: () -> ListWindow?) {
    val context = LocalPlatformContext.current
    val currentUrl by rememberUpdatedState(url)
    LaunchedEffect(key, context) {
        val prefetcher = Prefetcher(this, SingletonImageLoader.get(context), context) { currentUrl(it) }
        // The block reads one index, which changes when an item reaches the top, so it runs once per
        // item, never per scroll frame; the layout is read once per emission, outside it.
        snapshotFlow(first).collect { window()?.let(prefetcher::onWindow) }
    }
}

/** The items on screen, first and last, how many there are, and how many make a line. */
internal data class ListWindow(val first: Int, val last: Int, val count: Int, val span: Int = 1)

/** The queue behind [PrefetchImages], kept apart from Compose so its rules can be tested on their own. */
internal class Prefetcher(
    private val scope: CoroutineScope,
    private val loader: ImageLoader,
    private val context: PlatformContext,
    private val url: (Int) -> String?,
) {
    private val queue = ArrayDeque<Pending>()
    private val running = LinkedHashMap<Int, Job>()
    private val requested = LinkedHashSet<String>()
    private var previous: ListWindow? = null
    private var forward = true

    private class Pending(val index: Int, val url: String)

    fun onWindow(window: ListWindow) {
        val before = previous
        previous = window
        val heading = when {
            before == null -> forward
            window.first > before.first || window.last > before.last -> true
            window.first < before.first || window.last < before.last -> false
            else -> forward
        }
        if (heading != forward) {
            forward = heading
            // What is in flight finishes: its bytes serve the way back too. What waits is for the other way.
            queue.forEach { requested.remove(it.url) }
            queue.clear()
        }
        leaveToRows(window)

        val visible = window.last - window.first + 1
        val ahead = (visible * SCREENS_AHEAD).coerceIn(MIN_AHEAD, MAX_AHEAD)
        // Past the next line, which may hold up to a whole row of the grid after the last item on screen.
        val skip = (GAP_LINES - 1) * window.span + 1
        val indices = if (forward) {
            (window.last + skip)..minOf(window.last + skip + ahead - 1, window.count - 1)
        } else {
            (window.first - skip) downTo maxOf(window.first - skip - ahead + 1, 0)
        }
        for (index in indices) {
            val next = url(index) ?: continue
            if (!remember(next)) continue
            queue.addLast(Pending(index, next))
        }
        pump()
    }

    /** Items within a line of the screen are the rows' own to load: dropped while waiting, cancelled while fetching. */
    private fun leaveToRows(window: ListWindow) {
        val near = (window.first - window.span)..(window.last + window.span)
        queue.removeAll { it.index in near }
        running.keys.filter { it in near }.forEach { index -> running.remove(index)?.cancel() }
    }

    /** True the first time [url] is seen; the oldest are forgotten so a long session stays small. */
    private fun remember(url: String): Boolean {
        if (!requested.add(url)) return false
        if (requested.size > REMEMBERED) requested.remove(requested.first())
        return true
    }

    private fun pump() {
        while (running.size < IN_FLIGHT && queue.isNotEmpty()) {
            val next = queue.removeFirst()
            val job = scope.launch {
                try {
                    loader.execute(prefetchRequest(context, next.url))
                } finally {
                    if (running[next.index] === coroutineContext[Job]) running.remove(next.index)
                    pump()
                }
            }
            if (job.isActive) running[next.index] = job
        }
    }

    internal companion object {
        /** The lazy layout composes the next line itself; prefetch starts on the line after it. */
        const val GAP_LINES = 2
        const val SCREENS_AHEAD = 3
        const val MIN_AHEAD = 4
        const val MAX_AHEAD = 24

        /** Below the HTTP client's five calls per host, so the images on screen always have room. */
        const val IN_FLIGHT = 2
        const val REMEMBERED = 512

        fun prefetchRequest(context: PlatformContext, url: String): ImageRequest = ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .decoderFactory(BytesOnly)
            .build()
    }
}

/** A request [PrefetchImages] made: it fetches bytes and decodes nothing, so tooling that counts decodes skips it. */
val ImageRequest.isPrefetch: Boolean get() = decoderFactory === BytesOnly

/** Ends a prefetch once its bytes are in the disk cache: nothing is read back, decoded or kept. */
internal object BytesOnly : Decoder.Factory {
    private val nothing by lazy { Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8).asImage() }

    override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader) =
        Decoder { DecodeResult(image = nothing, isSampled = false) }
}
