package com.example.everything.core.image

import android.app.ActivityManager
import android.content.Context
import coil3.EventListener
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.bitmapFactoryMaxParallelism
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.memoryCacheMaxSizePercentWhileInBackground
import coil3.network.HttpException
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ErrorResult
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.serviceLoaderEnabled
import coil3.size.Size
import okhttp3.Call
import okio.Path.Companion.toOkioPath

/**
 * The app's one image loader, installed as Coil's singleton when the app starts. Every limit a
 * scrolling list depends on is set here rather than left to Coil's defaults:
 *
 * - A memory cache of a fifth of the app's heap class (three twentieths on a low-RAM phone), never
 *   more than 64 MB, cut to half while the app is in the background so the process is less likely
 *   to be killed there and started cold again.
 * - At most [DECODES_AT_ONCE] decodes at once, because a decode competes with the frame for the CPU,
 *   and none larger than the screen, which caps every request that resolves to the original size.
 * - Hardware bitmaps: the pixels reach the GPU once, when decoded, not in the frame that first draws them.
 * - No fade unless a request asks for one: in a flung list several would land at once, each redrawing
 *   its image on every frame of the fade.
 * - Coil's disk bounds, in the app's cache directory, which the system may clear and never backs up.
 *
 * [callFactory] is the one HTTP client the app's images and video share: the app's own, so they share
 * its connections and its certificate pins, or a [MediaCallFactory] when it has none. Coil never builds
 * a client of its own, and its service loader is off, so nothing joins the pipeline unseen: a decoder
 * for another format (SVG, GIF) is added here, explicitly. [events] lets dev tooling and the catalog's
 * perf log watch loads and decodes; production builds pass nothing.
 */
fun appImageLoader(
    context: PlatformContext,
    callFactory: () -> Call.Factory,
    events: EventListener.Factory? = null,
): ImageLoader = ImageLoader.Builder(context)
    .memoryCache { MemoryCache.Builder().maxSizeBytes { imageMemoryBytes(context) }.build() }
    .memoryCacheMaxSizePercentWhileInBackground(BACKGROUND_SHARE)
    .diskCache { DiskCache.Builder().directory(context.cacheDir.resolve(DISK_DIRECTORY).toOkioPath()).build() }
    .maxBitmapSize(screenSquare(context))
    .bitmapFactoryMaxParallelism(DECODES_AT_ONCE)
    .allowHardware(true)
    .crossfade(false)
    .serviceLoaderEnabled(false)
    .components { add(OkHttpNetworkFetcherFactory(callFactory = callFactory)) }
    .apply { if (events != null) eventListenerFactory(events) }
    .build()

/** The HTTP status a failed load got from its server, or null when it failed some other way. For dev tooling's logs. */
val ErrorResult.httpStatus: Int? get() = (throwable as? HttpException)?.response?.code

private fun imageMemoryBytes(context: Context): Long {
    val activities = context.getSystemService(ActivityManager::class.java)
    return imageMemoryBytes(activities.memoryClass, activities.isLowRamDevice)
}

/** A fifth of the heap class (three twentieths on a low-RAM phone), never more than [MAX_MEMORY_BYTES]. */
internal fun imageMemoryBytes(memoryClassMb: Int, lowRam: Boolean): Long =
    minOf((memoryClassMb * BYTES_PER_MB * if (lowRam) LOW_RAM_SHARE else MEMORY_SHARE).toLong(), MAX_MEMORY_BYTES)

/** The display's longest side, as a square: no decode is ever larger than the screen can show. */
private fun screenSquare(context: Context): Size {
    val metrics = context.resources.displayMetrics
    val side = maxOf(metrics.widthPixels, metrics.heightPixels)
    return Size(side, side)
}

/**
 * Coil's own default is four. The reference phone has two fast cores and six slow ones; the image A/B
 * there decides the value, and until it has run this is the middle of the three candidates (4, 2, 1).
 */
internal const val DECODES_AT_ONCE = 4
internal const val BACKGROUND_SHARE = 0.5
private const val MEMORY_SHARE = 0.20
private const val LOW_RAM_SHARE = 0.15
private const val MAX_MEMORY_BYTES = 64L * 1024 * 1024
private const val BYTES_PER_MB = 1024L * 1024

/** Coil's own folder name: an app that already ran Coil's default loader keeps the cache it has. */
private const val DISK_DIRECTORY = "coil3_disk_cache"
