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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Fetches the images of the items ahead of a scrolling list into the disk cache, so a fling meets
 * images that only need decoding. Bytes only: nothing is decoded or held in memory until a row asks
 * for its image at its own size. [url] gives an item's image, or null for an item without one.
 *
 * It starts two items past the last one on screen, because the list's own prefetch already composes
 * the next item and starts its decode at the right size, and reaches about three screens ahead in the
 * direction of travel. At most two requests run at once, so the images on screen never queue behind
 * it in the HTTP client; turning back drops what is queued, and leaving the screen cancels it.
 */
@Composable
fun PrefetchImages(state: LazyListState, url: (index: Int) -> String?) = PrefetchImages(state, url) {
    val info = state.layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) null else ListWindow(visible.first().index, visible.last().index, info.totalItemsCount)
}

/** [PrefetchImages] for a grid: the window is counted in items, so a grid of several columns reaches further. */
@Composable
fun PrefetchImages(state: LazyGridState, url: (index: Int) -> String?) = PrefetchImages(state, url) {
    val info = state.layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) null else ListWindow(visible.first().index, visible.last().index, info.totalItemsCount)
}

/** [PrefetchImages] for a pager, such as a video feed's posters. */
@Composable
fun PrefetchImages(state: PagerState, url: (page: Int) -> String?) = PrefetchImages(state, url) {
    val visible = state.layoutInfo.visiblePagesInfo
    if (visible.isEmpty()) null else ListWindow(visible.first().index, visible.last().index, state.pageCount)
}

@Composable
private fun PrefetchImages(key: Any, url: (Int) -> String?, window: () -> ListWindow?) {
    val context = LocalPlatformContext.current
    val currentUrl by rememberUpdatedState(url)
    LaunchedEffect(key, context) {
        val prefetcher = Prefetcher(this, SingletonImageLoader.get(context), context) { currentUrl(it) }
        // Emits when an item comes into view or leaves it, never per scroll frame.
        snapshotFlow(window).distinctUntilChanged().collect { it?.let(prefetcher::onWindow) }
    }
}

/** The items on screen, first and last, and how many there are. */
internal data class ListWindow(val first: Int, val last: Int, val count: Int)

/** The queue behind [PrefetchImages], kept apart from Compose so its rules can be tested on their own. */
internal class Prefetcher(
    private val scope: CoroutineScope,
    private val loader: ImageLoader,
    private val context: PlatformContext,
    private val url: (Int) -> String?,
) {
    private val queue = ArrayDeque<String>()
    private val requested = LinkedHashSet<String>()
    private var inFlight = 0
    private var previous: ListWindow? = null
    private var forward = true

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
            queue.forEach(requested::remove)
            queue.clear()
        }

        val visible = window.last - window.first + 1
        val ahead = (visible * SCREENS_AHEAD).coerceIn(MIN_AHEAD, MAX_AHEAD)
        val indices = if (forward) {
            (window.last + GAP)..minOf(window.last + ahead, window.count - 1)
        } else {
            (window.first - GAP) downTo maxOf(window.first - ahead, 0)
        }
        for (index in indices) {
            val next = url(index) ?: continue
            if (!remember(next)) continue
            queue.addLast(next)
        }
        pump()
    }

    /** True the first time [url] is seen; the oldest are forgotten so a long session stays small. */
    private fun remember(url: String): Boolean {
        if (!requested.add(url)) return false
        if (requested.size > REMEMBERED) requested.remove(requested.first())
        return true
    }

    private fun pump() {
        while (inFlight < IN_FLIGHT && queue.isNotEmpty()) {
            val next = queue.removeFirst()
            inFlight++
            scope.launch {
                try {
                    loader.execute(prefetchRequest(context, next))
                } finally {
                    inFlight--
                    pump()
                }
            }
        }
    }

    internal companion object {
        /** The lazy list composes the next item itself; prefetch starts one further on. */
        const val GAP = 2
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

/** Ends a prefetch once its bytes are in the disk cache: nothing is read back, decoded or kept. */
internal object BytesOnly : Decoder.Factory {
    private val nothing by lazy { Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8).asImage() }

    override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader) =
        Decoder { DecodeResult(image = nothing, isSampled = false) }
}
