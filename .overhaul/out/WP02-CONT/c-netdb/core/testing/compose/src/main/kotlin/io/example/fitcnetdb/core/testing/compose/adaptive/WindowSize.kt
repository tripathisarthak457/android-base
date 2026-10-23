package io.example.fitcnetdb.core.testing.compose.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The windows every screen is checked at on a pull request: each width class and each height
 * class at least once, from a phone held either way to a desktop window.
 */
enum class WindowSize(val width: Dp, val height: Dp) {
    Phone(411.dp, 891.dp),
    PhoneLandscape(891.dp, 411.dp),
    Foldable(673.dp, 841.dp),
    SmallTablet(1024.dp, 640.dp),
    Tablet(1280.dp, 800.dp),
    Desktop(1920.dp, 1080.dp),
    ;

    val size: DpSize get() = DpSize(width, height)
}
