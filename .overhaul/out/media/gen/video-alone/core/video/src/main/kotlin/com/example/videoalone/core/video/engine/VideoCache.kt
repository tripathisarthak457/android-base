@file:OptIn(UnstableApi::class)

package com.example.videoalone.core.video.engine

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.example.videoalone.core.video.VideoDefaults
import java.io.File

/**
 * The process's one cache of video bytes, in the app's cache directory, which the system may clear
 * and never backs up. Two caches on one directory is an error, so it is created once and never
 * released: releasing it needs disk work that a screen's teardown cannot wait for.
 */
internal object VideoCache {

    @Volatile
    private var cache: SimpleCache? = null

    /** Where the cache keeps its bytes, once it exists. */
    @Volatile
    var directory: File? = null
        private set

    fun get(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: File(context.applicationContext.cacheDir, DIRECTORY).let { folder ->
            SimpleCache(
                folder,
                LeastRecentlyUsedCacheEvictor(VideoDefaults.CACHE_BYTES),
                StandaloneDatabaseProvider(context.applicationContext),
            ).also {
                directory = folder
                cache = it
            }
        }
    }

    const val DIRECTORY = "video_cache"
}
