package com.example.everything.core.media.compress

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.example.everything.core.common.coroutines.AppDispatchers
import com.example.everything.core.common.log.Log
import com.example.everything.core.common.result.AppError
import com.example.everything.core.common.result.AppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/** Turns a picked or captured image into a file worth uploading or keeping. */
@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) {

    /**
     * Writes [source] to [destination] upright, no larger than [settings] allow, and without its
     * metadata (location, camera, time), which re-encoding drops. By default the copy goes under
     * `cacheDir/uploads/`, which this module's FileProvider can share.
     */
    suspend fun compress(
        source: Uri,
        settings: ImageCompression = ImageCompression.Photo,
        destination: File = newUpload(settings.format),
    ): AppResult<File> = try {
        val bytes = withContext(dispatchers.default) {
            val bitmap = decode(source, settings.maxDimension)
            try {
                encodeWithinBudget(bitmap, settings)
            } finally {
                bitmap.recycle()
            }
        }
        withContext(dispatchers.io) {
            destination.parentFile?.mkdirs()
            destination.writeBytes(bytes)
        }
        AppResult.Success(destination)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        // A file the picker can no longer read, an undecodable format or a full disk.
        Log.w("Image compression failed", failure, TAG)
        AppResult.Failure(AppError.Storage(failure))
    }

    private fun newUpload(format: ImageFormat): File =
        File(File(context.cacheDir, UPLOAD_DIRECTORY), "upload_${System.currentTimeMillis()}.${format.extension}")

    /**
     * Decodes at the target size rather than at full size and then scaling: a 48-megapixel photo
     * decoded whole is close to 200 MB of pixels. From API 28 the platform decoder also turns the
     * image upright by its EXIF orientation.
     */
    private fun decode(source: Uri, maxDimension: Int): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, source)) { decoder, info, _ ->
                val scale = min(1f, maxDimension.toFloat() / max(info.size.width, info.size.height))
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1),
                )
                // A hardware bitmap cannot be compressed.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            decodeBeforePie(source, maxDimension)
        }

    private fun decodeBeforePie(source: Uri, maxDimension: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
        }
        val sampled = context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("The image could not be decoded.")

        // Scaling and turning upright in one pass keeps at most two bitmaps alive at once.
        val matrix = orientationOf(source)
        val scale = min(1f, maxDimension.toFloat() / max(sampled.width, sampled.height))
        if (scale < 1f) matrix.postScale(scale, scale)
        if (matrix.isIdentity) return sampled
        return Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true).also {
            if (it !== sampled) sampled.recycle()
        }
    }

    private fun orientationOf(source: Uri): Matrix {
        val orientation = context.contentResolver.openInputStream(source)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        return Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    postRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    postRotate(270f)
                    postScale(-1f, 1f)
                }
            }
        }
    }

    /** Steps the quality down until the result fits [ImageCompression.maxBytes], or the floor is reached. */
    private fun encodeWithinBudget(bitmap: Bitmap, settings: ImageCompression): ByteArray {
        var quality = settings.quality
        var bytes = encode(bitmap, settings.format, quality)
        val budget = settings.maxBytes ?: return bytes
        while (bytes.size > budget && quality > settings.minQuality) {
            quality = (quality - QUALITY_STEP).coerceAtLeast(settings.minQuality)
            bytes = encode(bitmap, settings.format, quality)
        }
        return bytes
    }

    private fun encode(bitmap: Bitmap, format: ImageFormat, quality: Int): ByteArray =
        ByteArrayOutputStream().use { stream ->
            bitmap.compress(format.toCompressFormat(), quality, stream)
            stream.toByteArray()
        }

    private companion object {
        const val TAG = "ImageCompressor"
        const val UPLOAD_DIRECTORY = "uploads"
        const val QUALITY_STEP = 8

        /** The largest power of two that keeps the decoded image at or above the target. */
        fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
            var sample = 1
            while (width > 0 && height > 0 && max(width, height) / (sample * 2) >= maxDimension) sample *= 2
            return sample
        }
    }
}
