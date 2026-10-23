package com.example.videoalone.core.image

import android.app.ActivityManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.intercept.Interceptor
import coil3.network.NetworkFetcher
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.maxBitmapSize
import coil3.request.transitionFactory
import coil3.size.Size
import coil3.transition.Transition
import com.example.videoalone.core.testing.compose.TEST_SDK
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The limits a scrolling list depends on are the app's own, not Coil's defaults. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK], qualifiers = "w411dp-h891dp-xxhdpi")
class AppImageLoaderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the memory bound is a fifth of the heap class, three twentieths on a low-RAM phone, capped`() {
        assertEquals(53_687_091L, imageMemoryBytes(memoryClassMb = 256, lowRam = false))
        assertEquals(20_132_659L, imageMemoryBytes(memoryClassMb = 128, lowRam = true))
        assertEquals(64L * 1024 * 1024, imageMemoryBytes(memoryClassMb = 1024, lowRam = false))
    }

    @Test
    fun `the loader carries the app's limits`() {
        val loader = loader()
        val activities = context.getSystemService(ActivityManager::class.java)

        assertEquals(imageMemoryBytes(activities.memoryClass, activities.isLowRamDevice), loader.memoryCache!!.maxSize)
        assertEquals(DECODES_AT_ONCE, loader.coilOption("getBitmapFactoryMaxParallelism"))
        assertEquals(BACKGROUND_SHARE, loader.coilOption("getMemoryCacheMaxSizePercentWhileInBackground"))

        val request = loader.requestAsSeen()
        assertEquals(Transition.Factory.NONE, request.transitionFactory)
        assertTrue(request.allowHardware)
    }

    @Test
    fun `the largest decode is the display's longest side`() {
        // 891 dp at three pixels per dp: a decode that resolves to the original size stops at the screen.
        assertEquals(Size(2673, 2673), loader().requestAsSeen().maxBitmapSize)
    }

    @Test
    fun `the disk cache lives in the app's cache directory under Coil's own name, within Coil's bounds`() {
        val disk = loader().diskCache!!

        assertEquals(File(context.cacheDir, "coil3_disk_cache").canonicalFile, disk.directory.toFile().canonicalFile)
        assertTrue("${disk.maxSize}", disk.maxSize in 10L * 1024 * 1024..250L * 1024 * 1024)
    }

    @Test
    fun `nothing joins the pipeline through Coil's service loader`() {
        val loader = loader()

        assertEquals(false, loader.coilOption("getServiceLoaderEnabled"))
        assertEquals(1, loader.components.fetcherFactories.count { (factory, _) -> factory is NetworkFetcher.Factory })
    }

    @Test
    fun `images go through the client the app hands over`() {
        val seen = CopyOnWriteArrayList<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                seen += chain.request().url.toString()
                ok(chain.request())
            }
            .build()
        val loader = appImageLoader(context, callFactory = { client })

        runBlocking(Dispatchers.IO) {
            withTimeout(10_000) { loader.execute(ImageRequest.Builder(context).data("https://img.test/a.png").build()) }
        }

        assertEquals(listOf("https://img.test/a.png"), seen)
    }

    @Test
    fun `the shared client is built on the first request, once, and names the app`() {
        var built = 0
        val agents = CopyOnWriteArrayList<String?>()
        val factory = MediaCallFactory(userAgent = "VideoAloneTest (https://example.test)") { builder ->
            built++
            builder.addInterceptor { chain ->
                agents += chain.request().header("User-Agent")
                ok(chain.request())
            }.build()
        }
        assertEquals(0, built)

        val loader = appImageLoader(context, callFactory = { factory })
        runBlocking(Dispatchers.IO) {
            withTimeout(10_000) {
                loader.execute(ImageRequest.Builder(context).data("https://img.test/a.png").build())
                loader.execute(ImageRequest.Builder(context).data("https://img.test/b.png").build())
            }
        }

        assertEquals(1, built)
        assertEquals(listOf("VideoAloneTest (https://example.test)", "VideoAloneTest (https://example.test)"), agents)
    }

    private fun loader(): ImageLoader = appImageLoader(context, callFactory = { OkHttpClient() })

    private fun ok(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(ByteArray(0).toResponseBody("image/png".toMediaType()))
        .build()

    /** The request as the loader's pipeline sees it, with the loader's defaults applied, without fetching it. */
    private fun ImageLoader.requestAsSeen(): ImageRequest {
        var seen: ImageRequest? = null
        val stop = Interceptor { chain ->
            seen = chain.request
            ErrorResult(image = null, request = chain.request, throwable = IllegalStateException("stopped"))
        }
        val result = runBlocking { probed(stop).execute(ImageRequest.Builder(context).data("https://img.test/probe.png").build()) }
        return checkNotNull(seen) { "the request never reached the pipeline: ${(result as? ErrorResult)?.throwable}" }
    }

    /**
     * The same loader's options and caches with [interceptor] as its only component: the built loader's own
     * registry already ends in the interceptor that fetches, so anything appended to it would never run.
     */
    private fun ImageLoader.probed(interceptor: Interceptor): ImageLoader =
        newBuilder().components { add(interceptor) }.build()

    /**
     * Coil keeps these switches on its internal options and reads them through accessors of its own;
     * this reads them the same way, so the test sees what Coil sees.
     */
    private fun ImageLoader.coilOption(getter: String): Any? {
        val options = javaClass.getMethod("getOptions").invoke(this)
        val optionsClass = Class.forName("coil3.RealImageLoader\$Options")
        return listOf("coil3.ImageLoaders_androidKt", "coil3.ImageLoadersKt").firstNotNullOf { facade ->
            runCatching { Class.forName(facade).getMethod(getter, optionsClass).invoke(null, options) }.getOrNull()
        }
    }
}
