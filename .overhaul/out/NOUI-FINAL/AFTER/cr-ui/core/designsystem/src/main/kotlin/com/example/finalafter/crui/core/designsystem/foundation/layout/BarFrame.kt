package com.example.finalafter.crui.core.designsystem.foundation.layout

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The tab bar's frame in window pixels, for a sheet that rests on it. It never includes the
 * keyboard: a keyboard opened beneath a resting sheet must not move the line its detents are
 * measured from.
 */
@ShellSeamApi
@Stable
class BarFrame(private val settled: () -> Float, private val live: () -> Float) {
    /** The bar's top when shown in full and settled: detents resolve against it. Changes when the bar's size does. */
    val settledTop: Float get() = settled()

    /** The bar's top this frame, while it minimises on scroll or returns. Read in placement. */
    @get:FrequentlyChangingValue
    val liveTop: Float get() = live()
}

@ShellSeamApi
val LocalBarFrame: ProvidableCompositionLocal<BarFrame?> = staticCompositionLocalOf { null }
