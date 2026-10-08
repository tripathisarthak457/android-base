package com.base.app.core.ui

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.base.app.core.designsystem.theme.rememberReduceMotion

/** Where a Lottie animation comes from. */
sealed interface LottieSource {
    /** A file in `src/main/res/raw`, the usual home for an animation that ships with the app. */
    data class Raw(@param:RawRes val id: Int) : LottieSource

    /** A file in `src/main/assets`. */
    data class Asset(val fileName: String) : LottieSource

    /** Downloaded and cached by Lottie. Worth it only for animations that change without a release. */
    data class Url(val url: String) : LottieSource

    companion object {
        /** A ring that draws itself, then a check. Monochrome, so [AppLottie]'s tint colours it. */
        val Success: LottieSource = Raw(R.raw.lottie_success)
    }
}

/**
 * A Lottie animation. With the system's animations turned off it shows its final frame instead of
 * playing, so an animation that confirms something still says it.
 *
 * ```
 * AppLottie(LottieSource.Success, contentDescription = null, iterations = 1, tint = AppTheme.colors.success.content)
 * ```
 *
 * @param tint recolours every layer, for single-colour animations drawn in white. Leave it null
 *   for animations with their own palette.
 */
@Composable
fun AppLottie(
    source: LottieSource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    isPlaying: Boolean = true,
    speed: Float = 1f,
    tint: Color? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val reduceMotion = rememberReduceMotion()
    val composition by rememberLottieComposition(source.toSpec())
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        isPlaying = isPlaying && !reduceMotion,
        speed = speed,
    )
    val colourFilter = remember(tint) { tint?.let { SimpleColorFilter(it.toArgb()) } }
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(LottieProperty.COLOR_FILTER, colourFilter, "**"),
    )
    val described = if (contentDescription == null) {
        modifier
    } else {
        modifier.semantics { this.contentDescription = contentDescription }
    }

    // Until the file is parsed, and if it never is, the space stays reserved so nothing jumps.
    if (composition == null) {
        Box(described)
        return
    }
    LottieAnimation(
        composition = composition,
        progress = { if (reduceMotion) 1f else progress },
        modifier = described,
        dynamicProperties = if (colourFilter == null) null else dynamicProperties,
        contentScale = contentScale,
    )
}

private fun LottieSource.toSpec(): LottieCompositionSpec = when (this) {
    is LottieSource.Raw -> LottieCompositionSpec.RawRes(id)
    is LottieSource.Asset -> LottieCompositionSpec.Asset(fileName)
    is LottieSource.Url -> LottieCompositionSpec.Url(url)
}
