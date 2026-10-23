package com.example.videoalone.catalog.media.photos

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.example.videoalone.core.image.httpStatus
import com.example.videoalone.core.image.isPrefetch
import java.util.concurrent.atomic.AtomicInteger

/**
 * What the perf harness reads from the photo scenes, on the `perf.media` log tag, when the catalog is
 * launched with `--ez perfLog true`: each decode with its size and where it came from, each prefetch,
 * each HTTP error, and when image work starts and stops, so a measuring window can wait for quiet.
 * Silent otherwise: [events] gives the loader nothing to call.
 */
internal object MediaPerfLog : EventListener() {

    private val unfinished = AtomicInteger()

    fun enabled(intent: Intent?): Boolean = intent?.getBooleanExtra(EXTRA, false) == true

    fun events(intent: Intent?): EventListener.Factory? = if (enabled(intent)) listenerFactory else null

    private val listenerFactory = object : EventListener.Factory {
        override fun create(request: ImageRequest): EventListener = this@MediaPerfLog
    }

    fun log(line: String) {
        Log.i(TAG, line)
    }

    override fun onStart(request: ImageRequest) {
        if (unfinished.getAndIncrement() == 0) log("images busy t=${SystemClock.elapsedRealtime()}")
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        if (request.isPrefetch) {
            log("prefetched")
        } else {
            log("decoded w=${result.image.width} h=${result.image.height} source=${result.dataSource.name.lowercase()}")
        }
        finished()
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        result.httpStatus?.let { status -> log("http status=$status host=${Uri.parse(request.data.toString()).host}") }
        finished()
    }

    override fun onCancel(request: ImageRequest) = finished()

    private fun finished() {
        if (unfinished.decrementAndGet() == 0) log("images idle t=${SystemClock.elapsedRealtime()}")
    }

    const val TAG = "perf.media"
    private const val EXTRA = "perfLog"
}
