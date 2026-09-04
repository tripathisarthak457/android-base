package com.base.app.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.base.app.core.common.AppResult
import com.base.app.core.common.util.AppLogger
import com.base.app.core.coroutines.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/** A compressed image, ready to upload. */
data class CompressedImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val format: ImageFormat,
    val originalBytes: Long,
) {
    val sizeBytes: Int get() = bytes.size

    /** How much was saved, as a fraction. Useful in a log line while tuning settings. */
    val savedFraction: Float
        get() = if (originalBytes <= 0) 0f else 1f - (bytes.size.toFloat() / originalBytes)

    // Generated equals on a ByteArray compares references, which makes two identical results
    // unequal and quietly breaks caching and test assertions built on them.
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is CompressedImage &&
                width == other.width &&
                height == other.height &&
                format == other.format &&
                bytes.contentEquals(other.bytes)
            )

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + format.hashCode()
        return result
    }
}

/**
 * Turns a picked image into something worth uploading.
 *
 * ## Decoding happens twice, on purpose
 *
 * The first pass reads only the bounds (`inJustDecodeBounds`) to learn the dimensions without
 * allocating the pixels. That number chooses an `inSampleSize`, so the second pass decodes
 * straight to roughly the target size. Decoding a 12-megapixel photo at full resolution first
 * allocates ~48MB and is the single most common cause of an `OutOfMemoryError` in an app that
 * lets people attach photos — on the devices least able to afford it.
 *
 * `inSampleSize` only halves, so the result is then scaled precisely; halving alone would leave
 * an image up to twice the requested edge.
 *
 * ## Rotation is applied, not carried
 *
 * A camera photo is usually stored landscape with an EXIF orientation tag saying which way is up.
 * Strip the metadata without rotating the pixels and every portrait photo uploads sideways —
 * which is exactly the bug that makes people think an app is broken.
 */
@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun compress(
        uri: Uri,
        settings: ImageCompression = ImageCompression.Photo,
    ): AppResult<CompressedImage> = withContext(ioDispatcher) {
        runCatching {
            val originalSize = sizeOf(uri)
            val bounds = readBounds(uri)
            val bitmap = decodeScaled(uri, bounds, settings.maxDimension)
                ?: error("Could not decode the image.")

            val oriented = if (settings.stripMetadata) applyOrientation(uri, bitmap) else bitmap
            val scaled = scaleToFit(oriented, settings.maxDimension)
            val encoded = encodeWithinBudget(scaled, settings)

            if (scaled !== oriented) oriented.recycle()
            if (oriented !== bitmap) bitmap.recycle()
            scaled.recycle()

            AppResult.Success(
                CompressedImage(
                    bytes = encoded.first,
                    width = scaled.width,
                    height = scaled.height,
                    format = settings.format,
                    originalBytes = originalSize,
                ),
            )
        }.getOrElse { throwable ->
            AppLogger.e("Image compression failed", throwable, tag = TAG)
            AppResult.Failure(
                message = "Could not process that image.",
                cause = throwable,
            )
        }
    }

    /** Writes the compressed result to the cache directory, for an upload that needs a file. */
    suspend fun compressToFile(
        uri: Uri,
        settings: ImageCompression = ImageCompression.Photo,
        fileName: String = "upload_${System.currentTimeMillis()}",
    ): AppResult<File> = when (val result = compress(uri, settings)) {
        is AppResult.Success -> withContext(ioDispatcher) {
            runCatching {
                val file = File(context.cacheDir, "$fileName.${settings.format.extension}")
                file.writeBytes(result.data.bytes)
                AppResult.Success(file)
            }.getOrElse {
                AppResult.Failure(message = "Could not write the image.", cause = it)
            }
        }

        is AppResult.Failure -> result
    }

    private fun sizeOf(uri: Uri): Long =
        runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        }.getOrDefault(0L)

    private fun readBounds(uri: Uri): BitmapFactory.Options {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        return options
    }

    private fun decodeScaled(
        uri: Uri,
        bounds: BitmapFactory.Options,
        maxDimension: Int,
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
            // RGB_565 halves the memory but posterises gradients badly; the default ARGB_8888 is
            // worth the allocation now that inSampleSize has already capped it.
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    /**
     * The largest power-of-two divisor that keeps the image at or above the target.
     *
     * At or above, not below: sampling past the target and scaling *up* afterwards throws away
     * detail that cannot be recovered.
     */
    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        return sample
    }

    private fun applyOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToFit(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap

        val ratio = maxDimension.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).roundToInt().coerceAtLeast(1),
            (bitmap.height * ratio).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    /**
     * Encodes, stepping quality down until the result fits [ImageCompression.maxBytes].
     *
     * Linear steps rather than a binary search: the range is narrow, each attempt is fast at this
     * resolution, and a binary search on a non-monotonic-in-practice function is more code for no
     * measurable gain.
     */
    private fun encodeWithinBudget(
        bitmap: Bitmap,
        settings: ImageCompression,
    ): Pair<ByteArray, Int> {
        var quality = settings.quality
        var bytes = encode(bitmap, settings.format, quality)

        val budget = settings.maxBytes
        if (budget == null || !settings.format.isLossy) return bytes to quality

        while (bytes.size > budget && quality > settings.minQuality) {
            quality = (quality - QUALITY_STEP).coerceAtLeast(settings.minQuality)
            bytes = encode(bitmap, settings.format, quality)
        }

        if (bytes.size > budget) {
            AppLogger.w(
                "Image is ${bytes.size} bytes at quality $quality, over the $budget budget.",
                tag = TAG,
            )
        }
        return bytes to quality
    }

    private fun encode(bitmap: Bitmap, format: ImageFormat, quality: Int): ByteArray =
        ByteArrayOutputStream().use { stream ->
            bitmap.compress(format.toCompressFormat(), quality, stream)
            stream.toByteArray()
        }

    private companion object {
        const val TAG = "ImageCompressor"
        const val QUALITY_STEP = 8
    }
}
