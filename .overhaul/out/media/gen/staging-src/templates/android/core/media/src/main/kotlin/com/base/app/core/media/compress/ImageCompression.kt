package com.base.app.core.media.compress

import android.graphics.Bitmap
import android.os.Build

/** What "compress this image" means for one use. */
data class ImageCompression(
    /** Longest edge after scaling. The biggest lever on the final size. */
    val maxDimension: Int = 1920,
    /** 0 to 100. Below about 60 the artefacts show on flat areas such as skin and sky. */
    val quality: Int = 82,
    val format: ImageFormat = ImageFormat.Jpeg,
    /** A ceiling on the encoded size, met by lowering the quality as far as [minQuality]; null for none. */
    val maxBytes: Long? = null,
    val minQuality: Int = 45,
) {
    init {
        require(maxDimension > 0) { "maxDimension must be positive." }
        require(quality in 1..100) { "quality must be 1..100." }
        require(minQuality in 1..quality) { "minQuality must be between 1 and quality." }
    }

    companion object {
        /** Viewed full width on a phone and scrolled past. */
        val Photo = ImageCompression(maxDimension = 1920, quality = 82, maxBytes = 1024 * 1024)

        /** Shown at 40 to 96 dp, so more resolution is wasted bytes. */
        val Avatar = ImageCompression(maxDimension = 512, quality = 80, maxBytes = 120 * 1024)
    }
}

/** WebP is about a quarter smaller than JPEG at the same perceived quality, and every supported version decodes it. */
enum class ImageFormat(val mimeType: String, val extension: String) {
    Jpeg("image/jpeg", "jpg"),
    Webp("image/webp", "webp"),
    ;

    internal fun toCompressFormat(): Bitmap.CompressFormat = when (this) {
        Jpeg -> Bitmap.CompressFormat.JPEG
        // WEBP_LOSSY exists from API 30; below it the deprecated WEBP is the same lossy encoder.
        Webp -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
    }
}
