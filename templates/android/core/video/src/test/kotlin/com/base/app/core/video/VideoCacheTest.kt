@file:OptIn(UnstableApi::class)

package com.base.app.core.video

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceUtil
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.test.core.app.ApplicationProvider
import com.base.app.core.testing.compose.TEST_SDK
import com.base.app.core.video.engine.VideoCache
import com.base.app.core.video.engine.videoUpstream
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** One cache of video bytes per process, which a playing clip and a pre-cached one both fill. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class VideoCacheTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clip = ByteArray(CLIP_BYTES) { it.toByte() }
    private var fetches = 0

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            fetches++
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Length", "$CLIP_BYTES")
                .body(clip.toResponseBody())
                .build()
        }
        .build()

    private val spec = DataSpec.Builder().setUri(Uri.parse("https://v.test/clip.mp4")).setKey("clip").build()

    @Test
    fun `one video cache per process, in the app's cache directory`() {
        val first = VideoCache.get(context)

        assertSame(first, VideoCache.get(context))
        // The process may have opened it under an earlier test's cache directory, which that test's end removed.
        assertEquals(VideoCache.DIRECTORY, VideoCache.directory!!.name)
    }

    /** A cache of its own per test: the process's one cache outlives a test, and would carry bytes into the next. */
    private fun withCache(name: String, block: (SimpleCache) -> Unit) {
        val cache = SimpleCache(File(context.cacheDir, name), NoOpCacheEvictor(), StandaloneDatabaseProvider(context))
        try {
            block(cache)
        } finally {
            cache.release()
        }
    }

    @Test
    fun `a clip a player streams is written to the cache`() = withCache("streamed") { cache ->
        // What Media3's preload manager wraps around the engine's chain for playback: a reader that never writes.
        val playback = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(videoUpstream(cache) { client })
            .setCacheWriteDataSinkFactory(null)

        val source = playback.createDataSource()
        source.open(spec)
        DataSourceUtil.readToEnd(source)
        source.close()

        assertTrue(cache.isCached("clip", 0, CLIP_BYTES.toLong()))
        // A second play reads the disk.
        val again = playback.createDataSource()
        again.open(spec)
        DataSourceUtil.readToEnd(again)
        again.close()
        assertEquals(1, fetches)
    }

    @Test
    fun `pre-caching a far clip through the same chain finishes and fills the cache`() = withCache("precached") { cache ->
        // The manager's pre-cache path writes through a writer of its own over the engine's chain, whose
        // own writer then finds the span locked and must pass the bytes through rather than wait for it.
        val precache = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(videoUpstream(cache) { client })
            .createDataSource()
        val writer = CacheWriter(precache, spec.buildUpon().setKey("far").build(), null, null)

        val pool = Executors.newSingleThreadExecutor()
        try {
            pool.submit { writer.cache() }.get(5, TimeUnit.SECONDS)
        } finally {
            pool.shutdownNow()
        }

        assertTrue(cache.isCached("far", 0, CLIP_BYTES.toLong()))
    }

    private companion object {
        const val CLIP_BYTES = 64 * 1024
    }
}
