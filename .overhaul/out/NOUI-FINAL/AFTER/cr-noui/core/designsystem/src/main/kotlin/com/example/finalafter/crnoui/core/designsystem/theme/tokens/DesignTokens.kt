// The theme skeleton's values: palette, type scale, spacing, shapes, sizes, elevation and springs.
// Edit them here to change them app-wide. A value only one component uses belongs in its XDefaults.
package com.example.finalafter.crnoui.core.designsystem.theme.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finalafter.crnoui.core.designsystem.theme.AppColors
import com.example.finalafter.crnoui.core.designsystem.theme.StatusColors

internal object DesignTokens {

    object Spacing {
        val xxs: Dp = 2.dp
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 24.dp
        val xxl: Dp = 32.dp
        val xxxl: Dp = 48.dp
        val gutter: Dp = 16.dp
        val section: Dp = 24.dp
        val stack: Dp = 12.dp
    }

    object Radii {
        val none: Dp = 0.dp
        val xs: Dp = 6.dp
        val sm: Dp = 10.dp
        val md: Dp = 14.dp
        val lg: Dp = 20.dp
        val xl: Dp = 28.dp
    }

    object Sizes {
        val minTouchTarget: Dp = 48.dp
        val iconSmall: Dp = 16.dp
        val icon: Dp = 20.dp
        val iconLarge: Dp = 24.dp
        val buttonSmall: Dp = 32.dp
        val buttonMedium: Dp = 44.dp
        val buttonLarge: Dp = 52.dp
        val fieldHeight: Dp = 48.dp
        val rowHeight: Dp = 48.dp
        val topBarHeight: Dp = 52.dp
    }

    object Elevation {
        val none: Dp = 0.dp
        val raised: Dp = 1.dp
        val card: Dp = 3.dp
        val overlay: Dp = 8.dp
        val modal: Dp = 16.dp
    }

    /** One role of the type scale. */
    class TypeRole(val size: TextUnit, val lineHeight: TextUnit, val tracking: TextUnit, val weight: Int)

    object Type {
        val largeTitle = TypeRole(34.sp, 41.sp, (-0.6).sp, 700)
        val title1 = TypeRole(28.sp, 34.sp, (-0.5).sp, 700)
        val title2 = TypeRole(22.sp, 28.sp, (-0.35).sp, 700)
        val title3 = TypeRole(20.sp, 25.sp, (-0.25).sp, 600)
        val headline = TypeRole(17.sp, 22.sp, (-0.15).sp, 600)
        val body = TypeRole(17.sp, 22.sp, (-0.15).sp, 400)
        val callout = TypeRole(16.sp, 21.sp, (-0.1).sp, 400)
        val subheadline = TypeRole(15.sp, 20.sp, (-0.05).sp, 400)
        val footnote = TypeRole(13.sp, 18.sp, 0.sp, 400)
        val caption1 = TypeRole(12.sp, 16.sp, 0.05.sp, 400)
        val caption2 = TypeRole(11.sp, 13.sp, 0.1.sp, 500)
        val mono = TypeRole(15.sp, 20.sp, 0.sp, 400)
    }

    /** A spring in iOS's terms: seconds to settle, and how far it overshoots (negative: overdamped). */
    class Spring(val response: Float, val bounce: Float)

    object Motion {
        val press = Spring(0.16f, 0.0f)
        val release = Spring(0.32f, 0.15f)
        val thumb = Spring(0.28f, 0.0f)
        val screen = Spring(0.344f, 0.0f)
        val sheet = Spring(0.34f, 0.0f)
        val dialog = Spring(0.275f, 0.0f)
        val layout = Spring(0.36f, 0.0f)
        val emphasis = Spring(0.5f, 0.15f)
        val interactive = Spring(0.15f, 0.14f)
        val bounceBack = Spring(0.444f, -0.1f)

        const val visibilityThreshold: Float = 0.001f
        const val flingFriction: Float = 0.4767f
        const val pagingFriction: Float = 2.393f
        const val fadeShortMs: Int = 120
        const val fadeLongMs: Int = 200
        val fadeEnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
        val fadeExitEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    }

    object Palette {
        val light = AppColors(
            background = Color(0xFFF7F8FA),
            backgroundGrouped = Color(0xFFF2F3F7),
            surface = Color(0xFFFFFFFF),
            surfaceGrouped = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFFFFFFF),
            surfaceInverse = Color(0xFF0B0F1A),
            fill = Color(0xFFE9ECF1),
            segmentThumb = Color(0xFFFFFFFF),
            separator = Color(0xFFE3E6EC),
            outline = Color(0xFF838B99),
            contentPrimary = Color(0xFF0B0F1A),
            contentSecondary = Color(0xFF3D4552),
            contentTertiary = Color(0xFF626A78),
            contentDisabled = Color(0xFFA3AAB6),
            contentInverse = Color(0xFFFFFFFF),
            highlight = Color(0x120B0F1A),
            scrim = Color(0x660B0F1A),
            focusRing = Color(0xFF2C6BED),
            shadow = Color(0x240B0F1A),
            accent = Color(0xFF2C6BED),
            accentPressed = Color(0xFF1F52C4),
            accentSubtle = Color(0xFFE8EFFD),
            onAccent = Color(0xFFFFFFFF),
            secondary = Color(0xFF617DB8),
            secondaryPressed = Color(0xFF445F97),
            secondarySubtle = Color(0xFFEFF2F8),
            onSecondary = Color(0xFF0B0E14),
            tertiary = Color(0xFFAE2CED),
            tertiaryPressed = Color(0xFF8E11CA),
            tertiarySubtle = Color(0xFFF7EAFD),
            onTertiary = Color(0xFFFFFFFF),
            accentContent = Color(0xFF2863DD),
            success = StatusColors(base = Color(0xFF117C59), content = Color(0xFF117C59), subtle = Color(0xFFE4F5EE), on = Color(0xFFFFFFFF)),
            warning = StatusColors(base = Color(0xFFA35B00), content = Color(0xFFA35B00), subtle = Color(0xFFFDF1E0), on = Color(0xFFFFFFFF)),
            danger = StatusColors(base = Color(0xFFC42B32), content = Color(0xFFC42B32), subtle = Color(0xFFFCE9E9), on = Color(0xFFFFFFFF)),
            info = StatusColors(base = Color(0xFF1E5FBF), content = Color(0xFF1E5FBF), subtle = Color(0xFFE7F0FD), on = Color(0xFFFFFFFF)),
            neutral = StatusColors(base = Color(0xFF5A6472), content = Color(0xFF5A6472), subtle = Color(0xFFEFF1F5), on = Color(0xFFFFFFFF)),
            isLight = true,
        )
        val dark = AppColors(
            background = Color(0xFF0B0E14),
            backgroundGrouped = Color(0xFF0B0E14),
            surface = Color(0xFF141922),
            surfaceGrouped = Color(0xFF141922),
            surfaceElevated = Color(0xFF1D232E),
            surfaceInverse = Color(0xFFF2F4F8),
            fill = Color(0xFF262D3A),
            segmentThumb = Color(0xFF414957),
            separator = Color(0xFF262D3A),
            outline = Color(0xFF687183),
            contentPrimary = Color(0xFFF2F4F8),
            contentSecondary = Color(0xFFB4BCCA),
            contentTertiary = Color(0xFF8A93A3),
            contentDisabled = Color(0xFF565F6E),
            contentInverse = Color(0xFF0B0E14),
            highlight = Color(0x17FFFFFF),
            scrim = Color(0x8F000000),
            focusRing = Color(0xFF5B8DEF),
            shadow = Color(0x66000000),
            accent = Color(0xFF5B8DEF),
            accentPressed = Color(0xFF7BA5F5),
            accentSubtle = Color(0xFF16233B),
            onAccent = Color(0xFF06101F),
            secondary = Color(0xFF879BC5),
            secondaryPressed = Color(0xFFA4B3D0),
            secondarySubtle = Color(0xFF171F31),
            onSecondary = Color(0xFF06101F),
            tertiary = Color(0xFFBE60EB),
            tertiaryPressed = Color(0xFFCB89EC),
            tertiarySubtle = Color(0xFF221438),
            onTertiary = Color(0xFF06101F),
            accentContent = Color(0xFF5B8DEF),
            success = StatusColors(base = Color(0xFF3DD4A0), content = Color(0xFF3DD4A0), subtle = Color(0xFF10271F), on = Color(0xFF06101F)),
            warning = StatusColors(base = Color(0xFFE9A23B), content = Color(0xFFE9A23B), subtle = Color(0xFF2A1F0E), on = Color(0xFF06101F)),
            danger = StatusColors(base = Color(0xFFF0666D), content = Color(0xFFF0666D), subtle = Color(0xFF2B1416), on = Color(0xFF06101F)),
            info = StatusColors(base = Color(0xFF6FA6FF), content = Color(0xFF6FA6FF), subtle = Color(0xFF10203A), on = Color(0xFF06101F)),
            neutral = StatusColors(base = Color(0xFFB4BCCA), content = Color(0xFFB4BCCA), subtle = Color(0xFF1D232E), on = Color(0xFF06101F)),
            isLight = false,
        )
    }

    /** The project's own choices, which `AppTheme` falls back to. */
    object Defaults {
        const val hapticsEnabled: Boolean = true
        const val fontName: String = "DM Sans"
        const val monoFontName: String = "JetBrains Mono"
    }
}
