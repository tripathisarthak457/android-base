package com.base.app.core.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.base.app.core.common.AppResult
import com.base.app.core.common.getOrNull
import com.base.app.core.common.util.AppLogger
import com.base.app.core.coroutines.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** What a video actually is, before deciding whether it needs re-encoding. */
data class VideoInfo(
    val widthPx: Int,
    val heightPx: Int,
    val durationMillis: Long,
    val bitRate: Int,
    val sizeBytes: Long,
    val mimeType: String?,
    val rotationDegrees: Int,
) {
    /** Width and height as the video will be *displayed*. */
    val displayWidth: Int get() = if (rotationDegrees % 180 == 90) heightPx else widthPx
    val displayHeight: Int get() = if (rotationDegrees % 180 == 90) widthPx else heightPx

    fun needsCompression(settings: VideoCompression): Boolean {
        val maxDurationMillis = settings.maxDurationSeconds?.times(MILLIS_PER_SECOND)
        return maxOf(displayWidth, displayHeight) > settings.maxDimension ||
            bitRate > settings.bitRate ||
            (maxDurationMillis != null && durationMillis > maxDurationMillis)
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}

/** Re-encodes a video to fit [VideoCompression]. */
interface VideoTranscoder {

    suspend fun transcode(
        uri: Uri,
        settings: VideoCompression = VideoCompression.Standard,
    ): AppResult<File>
}

/**
 * Copies the file and says so. A no-op that reports honestly beats one that silently pretends: the
 * log line is what tells you, the first time a 90MB upload is slow, that no transcoder is bound.
 */
@Singleton
class PassthroughVideoTranscoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val probe: VideoProbe,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : VideoTranscoder {

    override suspend fun transcode(
        uri: Uri,
        settings: VideoCompression,
    ): AppResult<File> = withContext(ioDispatcher) {
        runCatching {
            val info = probe.inspect(uri).getOrNull()
            if (info?.needsCompression(settings) == true) {
                AppLogger.w(
                    "Video is ${info.displayWidth}x${info.displayHeight} at ${info.bitRate}bps " +
                        "(${info.sizeBytes / BYTES_PER_KB}KB) and exceeds the requested settings, but no " +
                        "VideoTranscoder is bound — uploading it unchanged. Bind one in your app " +
                        "module, or constrain capture with MediaPicker.recordVideo.",
                    tag = TAG,
                )
            }

            val target = File(context.cacheDir, "video_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use(input::copyTo)
            } ?: error("Could not open the video.")

            AppResult.Success(target)
        }.getOrElse { throwable ->
            AppLogger.e("Video copy failed", throwable, tag = TAG)
            AppResult.Failure(message = "Could not process that video.", cause = throwable)
        }
    }

    private companion object {
        const val TAG = "VideoTranscoder"
        const val BYTES_PER_KB = 1_024
    }
}

/** Reads a video's dimensions, duration and bit rate without decoding it. */
@Singleton
class VideoProbe @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun inspect(uri: Uri): AppResult<VideoInfo> = withContext(ioDispatcher) {
        // Released in a `finally` rather than with `use`: MediaMetadataRetriever only became
        // AutoCloseable in API 29, and `use` on an older release fails at verification rather than
        // at the call.
        val retriever = MediaMetadataRetriever()
        runCatching {
            try {
                retriever.setDataSource(context, uri)

                fun read(key: Int): String? = retriever.extractMetadata(key)

                AppResult.Success(
                    VideoInfo(
                        widthPx = read(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                        heightPx = read(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                        durationMillis = read(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                        bitRate = read(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0,
                        sizeBytes = context.contentResolver
                            .openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L,
                        mimeType = read(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                        rotationDegrees = read(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION,
                        )?.toIntOrNull() ?: 0,
                    ),
                )
            } finally {
                retriever.release()
            }
        }.getOrElse { throwable ->
            AppLogger.e("Could not read video metadata", throwable, tag = "VideoProbe")
            AppResult.Failure(message = "Could not read that video.", cause = throwable)
        }
    }
}
