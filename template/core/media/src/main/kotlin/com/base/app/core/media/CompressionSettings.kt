package com.base.app.core.media

import android.graphics.Bitmap
import android.os.Build

/** What "compress this image" should mean for a given upload. */
data class ImageCompression(
    /** Longest edge after scaling. The single biggest lever on the final size. */
    val maxDimension: Int = 1920,

    /** 0-100. Below about 60 the artefacts are visible on flat areas such as skin and sky. */
    val quality: Int = 82,

    val format: ImageFormat = ImageFormat.Jpeg,

    /** Hard ceiling on the encoded size, or null for none. */
    val maxBytes: Long? = null,

    /** The floor the byte-budget search will not go below, however large the file stays. */
    val minQuality: Int = 45,

    /** Strip location, device model and timestamps. On by default. */
    val stripMetadata: Boolean = true,
) {
    init {
        require(maxDimension > 0) { "maxDimension must be positive." }
        require(quality in 1..100) { "quality must be 1..100." }
        require(minQuality in 1..quality) { "minQuality must be between 1 and quality." }
    }

    companion object {
        /** Small and square-ish. Displayed at 40-96dp, so resolution beyond this is wasted bytes. */
        val Avatar = ImageCompression(maxDimension = 512, quality = 80, maxBytes = 120 * 1024)

        /** Has to stay legible when the reviewer zooms in. High edge, high quality. */
        val Document = ImageCompression(maxDimension = 2560, quality = 90, maxBytes = 2 * 1024 * 1024)

        /** Viewed full-width on a phone and scrolled past. */
        val Photo = ImageCompression(maxDimension = 1920, quality = 82, maxBytes = 1024 * 1024)

        /** For a preview in a list. Deliberately tiny. */
        val Thumbnail = ImageCompression(maxDimension = 320, quality = 75)
    }
}

/**
 * The encoders worth offering. WebP is smaller than JPEG at the same perceived quality — typically
 * 25-30% — and is supported everywhere this project runs.
 */
enum class ImageFormat(val mimeType: String, val extension: String) {
    Jpeg("image/jpeg", "jpg"),
    Webp("image/webp", "webp"),
    Png("image/png", "png"),
    ;

    @Suppress("DEPRECATION")
    internal fun toCompressFormat(): Bitmap.CompressFormat = when (this) {
        Jpeg -> Bitmap.CompressFormat.JPEG
        // WEBP_LOSSY only exists from API 30. The deprecated WEBP is the same lossy encoder on
        // older releases — deprecated because it became ambiguous once the lossless variant was
        // added, not because it stopped working.
        Webp -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

        Png -> Bitmap.CompressFormat.PNG
    }

    /** PNG has no quality dial; iterating on it would loop without ever shrinking the file. */
    internal val isLossy: Boolean get() = this != Png
}

/** What "compress this video" should mean. */
data class VideoCompression(
    /** Longest edge. 720p is the sweet spot for anything not being watched on a television. */
    val maxDimension: Int = 1280,

    /** Bits per second. ~2 Mbps at 720p is visually clean for typical hand-held footage. */
    val bitRate: Int = 2_000_000,

    val frameRate: Int = 30,

    /** Hard cap on duration; the capture intent enforces it for free. */
    val maxDurationSeconds: Int? = null,

    val codec: VideoCodec = VideoCodec.H264,
) {
    companion object {
        /** Small enough to upload on a phone connection. */
        val Standard = VideoCompression()

        /** For a short clip attached to a message. */
        val Message = VideoCompression(
            maxDimension = 854,
            bitRate = 1_200_000,
            maxDurationSeconds = 60,
        )

        val HighQuality = VideoCompression(maxDimension = 1920, bitRate = 5_000_000)
    }
}

/**
 * H.264 plays everywhere. HEVC is roughly half the size at the same quality and is *not*
 * universally decodable — notably by some backends and by older desktop browsers — so it is worth
 * choosing deliberately rather than by default.
 */
enum class VideoCodec(val mimeType: String) {
    H264("video/avc"),
    Hevc("video/hevc"),
}
