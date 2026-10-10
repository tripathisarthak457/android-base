// Generated from tokens/*.json by generator/genkit/tokens. Do not edit; run python3 -m genkit.tokens --write.
package com.base.app.core.designsystem.theme.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.base.app.core.designsystem.theme.AppDesignStyle
import com.base.app.core.designsystem.theme.AppMotionStyle
import com.base.app.core.designsystem.theme.BarTreatment
import com.base.app.core.designsystem.theme.CardTreatment
import com.base.app.core.designsystem.theme.FieldTreatment
import com.base.app.core.designsystem.theme.SurfaceTone
import com.base.app.core.designsystem.theme.TypeVoice

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
        val sheet: Dp = 24.dp
    }

    object Sizes {
        val minTouchTarget: Dp = 48.dp
        val iconSmall: Dp = 16.dp
        val icon: Dp = 20.dp
        val iconLarge: Dp = 24.dp
        val buttonSmall: Dp = 36.dp
        val buttonMedium: Dp = 44.dp
        val buttonLarge: Dp = 52.dp
        val fieldHeight: Dp = 52.dp
        val topBarHeight: Dp = 56.dp
        val tabBarHeight: Dp = 64.dp
        val tabBarSideMargin: Dp = 20.dp
        val tabBarLift: Dp = 12.dp
        val tabBarMaxWidth: Dp = 480.dp
    }

    object Borders {
        /** Physical pixels, not dp: a hairline is one device pixel at every density. */
        const val hairlinePx: Int = 1
        val width: Dp = 1.dp
        val widthStrong: Dp = 1.5.dp
    }

    object Elevation {
        val none: Dp = 0.dp
        val raised: Dp = 1.dp
        val card: Dp = 3.dp
        val overlay: Dp = 8.dp
        val modal: Dp = 16.dp
    }

    /** One role of the type scale. Each voice changes the weight only. */
    class TypeRole(
        val size: TextUnit,
        val lineHeight: TextUnit,
        val tracking: TextUnit,
        val weightNeutral: Int,
        val weightBold: Int,
        val weightEditorial: Int,
        val weightRounded: Int,
    )

    object Type {
        val largeTitle = TypeRole(34.sp, 41.sp, (-0.8).sp, 700, 900, 600, 900)
        val title1 = TypeRole(28.sp, 34.sp, (-0.6).sp, 700, 900, 600, 900)
        val title2 = TypeRole(22.sp, 28.sp, (-0.5).sp, 700, 800, 600, 900)
        val title3 = TypeRole(20.sp, 25.sp, (-0.3).sp, 600, 800, 600, 800)
        val headline = TypeRole(17.sp, 22.sp, (-0.2).sp, 600, 800, 600, 800)
        val body = TypeRole(17.sp, 22.sp, 0.sp, 400, 400, 400, 400)
        val callout = TypeRole(16.sp, 21.sp, 0.sp, 400, 400, 400, 400)
        val subheadline = TypeRole(15.sp, 20.sp, 0.sp, 400, 400, 400, 400)
        val footnote = TypeRole(13.sp, 18.sp, 0.sp, 400, 500, 400, 500)
        val caption1 = TypeRole(12.sp, 16.sp, 0.1.sp, 400, 500, 400, 500)
        val caption2 = TypeRole(11.sp, 13.sp, 0.2.sp, 500, 600, 500, 600)
        val mono = TypeRole(15.sp, 20.sp, 0.sp, 400, 400, 400, 400)
    }

    /** A spring as Compose takes it: mass 1. */
    class Spring(val stiffness: Float, val dampingRatio: Float)

    class MotionStyle(
        val press: Spring,
        val release: Spring,
        val selection: Spring,
        val screen: Spring,
        val sheet: Spring,
        val layout: Spring,
        val emphasis: Spring,
    )

    object Motion {
        val standard = MotionStyle(
            press = Spring(1542.13f, 1.0f),
            release = Spring(385.53f, 0.85f),
            selection = Spring(438.65f, 0.91f),
            screen = Spring(685.39f, 1.0f),
            sheet = Spring(341.51f, 1.0f),
            layout = Spring(304.62f, 1.0f),
            emphasis = Spring(157.91f, 0.85f),
        )
        val bouncy = MotionStyle(
            press = Spring(1542.13f, 1.0f),
            release = Spring(385.53f, 0.65f),
            selection = Spring(438.65f, 0.79f),
            screen = Spring(685.39f, 1.0f),
            sheet = Spring(341.51f, 1.0f),
            layout = Spring(304.62f, 1.0f),
            emphasis = Spring(157.91f, 0.65f),
        )
        val calm = MotionStyle(
            press = Spring(1542.13f, 1.0f),
            release = Spring(385.53f, 1.0f),
            selection = Spring(438.65f, 1.0f),
            screen = Spring(518.77f, 1.0f),
            sheet = Spring(258.49f, 1.0f),
            layout = Spring(230.56f, 1.0f),
            emphasis = Spring(157.91f, 1.0f),
        )
        val snappy = MotionStyle(
            press = Spring(1542.13f, 1.0f),
            release = Spring(385.53f, 0.9f),
            selection = Spring(438.65f, 0.94f),
            screen = Spring(906.43f, 1.0f),
            sheet = Spring(451.65f, 1.0f),
            layout = Spring(402.86f, 1.0f),
            emphasis = Spring(157.91f, 0.9f),
        )

        const val visibilityThreshold: Float = 0.001f
        const val offsetVisibilityThresholdPx: Int = 1
        const val fadeShortMs: Int = 120
        const val fadeLongMs: Int = 200
        const val staggerStepMs: Int = 24
        const val staggerMaxItems: Int = 6
        const val pressScale: Float = 0.96f
        const val pressScaleLarge: Float = 0.98f
        const val pressScaleIcon: Float = 0.9f
        const val pressDimAlpha: Float = 0.45f
        const val highlightFadeOutMs: Int = 220
        const val navigationParallax: Float = 0.3f
        const val navigationScrimAlphaLight: Float = 0.1f
        const val navigationScrimAlphaDark: Float = 0.24f
        val navigationEdgeShadow: Dp = 8.dp
    }

    class Status(val base: Color, val content: Color, val subtle: Color, val border: Color, val on: Color)

    class Roles(
        val background: Color,
        val surface: Color,
        val surfaceVariant: Color,
        val surfaceInverse: Color,
        val contentPrimary: Color,
        val contentSecondary: Color,
        val contentTertiary: Color,
        val contentDisabled: Color,
        val contentInverse: Color,
        val border: Color,
        val borderStrong: Color,
        val divider: Color,
        val backgroundGrouped: Color,
        val surfaceGrouped: Color,
        val surfaceElevated: Color,
        val fill: Color,
        val separator: Color,
        val highlight: Color,
        val skeleton: Color,
        val scrim: Color,
        val shadow: Color,
        val focusRing: Color,
        val accent: Color,
        val accentPressed: Color,
        val accentSubtle: Color,
        val accentContent: Color,
        val onAccent: Color,
        val secondary: Color,
        val secondaryPressed: Color,
        val secondarySubtle: Color,
        val secondaryContent: Color,
        val onSecondary: Color,
        val tertiary: Color,
        val tertiaryPressed: Color,
        val tertiarySubtle: Color,
        val tertiaryContent: Color,
        val onTertiary: Color,
        val success: Status,
        val warning: Status,
        val danger: Status,
        val info: Status,
        val neutral: Status,
    )

    object Palette {
        val grey0 = Color(0xFFFFFFFF)
        val grey50 = Color(0xFFF7F8FA)
        val grey100 = Color(0xFFEFF1F5)
        val grey150 = Color(0xFFE9ECF1)
        val grey200 = Color(0xFFE3E6EC)
        val grey300 = Color(0xFFCBD1DB)
        val grey400 = Color(0xFFA3AAB6)
        val grey500 = Color(0xFF6B7280)
        val grey600 = Color(0xFF5A6472)
        val grey700 = Color(0xFF3D4552)
        val grey900 = Color(0xFF0B0F1A)
        val ink100 = Color(0xFFF2F4F8)
        val ink200 = Color(0xFFB4BCCA)
        val ink300 = Color(0xFF7F8899)
        val ink400 = Color(0xFF565F6E)
        val ink500 = Color(0xFF38414F)
        val ink600 = Color(0xFF262D3A)
        val ink700 = Color(0xFF1D232E)
        val ink800 = Color(0xFF141922)
        val ink900 = Color(0xFF0B0E14)

        object Cool {
            val light = Roles(
                background = Color(0xFFF7F8FA),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFEFF1F5),
                surfaceInverse = Color(0xFF0B0F1A),
                contentPrimary = Color(0xFF0B0F1A),
                contentSecondary = Color(0xFF3D4552),
                contentTertiary = Color(0xFF6B7280),
                contentDisabled = Color(0xFFA3AAB6),
                contentInverse = Color(0xFFFFFFFF),
                border = Color(0xFFE3E6EC),
                borderStrong = Color(0xFFCBD1DB),
                divider = Color(0xFFE9ECF1),
                backgroundGrouped = Color(0xFFEFF1F5),
                surfaceGrouped = Color(0xFFFFFFFF),
                surfaceElevated = Color(0xFFFFFFFF),
                fill = Color(0xFFE9ECF1),
                separator = Color(0xFFE3E6EC),
                highlight = Color(0xFFE9ECF1),
                skeleton = Color(0xFFE9ECF1),
                scrim = Color(0x730B0F1A),
                shadow = Color(0x1F000000),
                focusRing = Color(0x662C6BED),
                accent = Color(0xFF2C6BED),
                accentPressed = Color(0xFF1F52C4),
                accentSubtle = Color(0xFFE8EFFD),
                accentContent = Color(0xFF1C60EC),
                onAccent = Color(0xFFFFFFFF),
                secondary = Color(0xFF617DB8),
                secondaryPressed = Color(0xFF445F97),
                secondarySubtle = Color(0xFFEFF2F8),
                secondaryContent = Color(0xFF4D6BAB),
                onSecondary = Color(0xFF0B0E14),
                tertiary = Color(0xFFAE2CED),
                tertiaryPressed = Color(0xFF8E11CA),
                tertiarySubtle = Color(0xFFF7EAFD),
                tertiaryContent = Color(0xFFAE2CED),
                onTertiary = Color(0xFFFFFFFF),
                success = Status(Color(0xFF117C59), Color(0xFF117C59), Color(0xFFE4F5EE), Color(0xFFB7E2D2), Color(0xFFFFFFFF)),
                warning = Status(Color(0xFFA35B00), Color(0xFFA35B00), Color(0xFFFDF1E0), Color(0xFFF2DDB8), Color(0xFFFFFFFF)),
                danger = Status(Color(0xFFC42B32), Color(0xFFC42B32), Color(0xFFFCE9E9), Color(0xFFF3C9CB), Color(0xFFFFFFFF)),
                info = Status(Color(0xFF1E5FBF), Color(0xFF1E5FBF), Color(0xFFE7F0FD), Color(0xFFC3D8F7), Color(0xFFFFFFFF)),
                neutral = Status(Color(0xFF5A6472), Color(0xFF5A6472), Color(0xFFEFF1F5), Color(0xFFE3E6EC), Color(0xFFFFFFFF)),
            )
            val dark = Roles(
                background = Color(0xFF0B0E14),
                surface = Color(0xFF141922),
                surfaceVariant = Color(0xFF1D232E),
                surfaceInverse = Color(0xFFF2F4F8),
                contentPrimary = Color(0xFFF2F4F8),
                contentSecondary = Color(0xFFB4BCCA),
                contentTertiary = Color(0xFF7F8899),
                contentDisabled = Color(0xFF565F6E),
                contentInverse = Color(0xFF0B0E14),
                border = Color(0xFF262D3A),
                borderStrong = Color(0xFF38414F),
                divider = Color(0xFF1E2531),
                backgroundGrouped = Color(0xFF0B0E14),
                surfaceGrouped = Color(0xFF141922),
                surfaceElevated = Color(0xFF1D232E),
                fill = Color(0xFF1D232E),
                separator = Color(0xFF262D3A),
                highlight = Color(0xFF262D3A),
                skeleton = Color(0xFF1D232E),
                scrim = Color(0x99000000),
                shadow = Color(0x66000000),
                focusRing = Color(0x735B8DEF),
                accent = Color(0xFF5B8DEF),
                accentPressed = Color(0xFF7BA5F5),
                accentSubtle = Color(0xFF16233B),
                accentContent = Color(0xFF5B8DEF),
                onAccent = Color(0xFF06101F),
                secondary = Color(0xFF879BC5),
                secondaryPressed = Color(0xFFA4B3D0),
                secondarySubtle = Color(0xFF171F31),
                secondaryContent = Color(0xFF879BC5),
                onSecondary = Color(0xFF06101F),
                tertiary = Color(0xFFBE60EB),
                tertiaryPressed = Color(0xFFCB89EC),
                tertiarySubtle = Color(0xFF221438),
                tertiaryContent = Color(0xFFBE60EB),
                onTertiary = Color(0xFF06101F),
                success = Status(Color(0xFF3DD4A0), Color(0xFF3DD4A0), Color(0xFF10271F), Color(0xFF1E4638), Color(0xFF06101F)),
                warning = Status(Color(0xFFE9A23B), Color(0xFFE9A23B), Color(0xFF2A1F0E), Color(0xFF4A3616), Color(0xFF06101F)),
                danger = Status(Color(0xFFF0666D), Color(0xFFF0666D), Color(0xFF2B1416), Color(0xFF4E2126), Color(0xFF06101F)),
                info = Status(Color(0xFF6FA6FF), Color(0xFF6FA6FF), Color(0xFF10203A), Color(0xFF1E3862), Color(0xFF06101F)),
                neutral = Status(Color(0xFFB4BCCA), Color(0xFFB4BCCA), Color(0xFF1A202B), Color(0xFF2C3441), Color(0xFF06101F)),
            )
        }

        object BrandTint {
            val light = Roles(
                background = Color(0xFFEEF2FA),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE1E8F6),
                surfaceInverse = Color(0xFF0B0F1A),
                contentPrimary = Color(0xFF0B0F1A),
                contentSecondary = Color(0xFF3D4552),
                contentTertiary = Color(0xFF6B7280),
                contentDisabled = Color(0xFFA3AAB6),
                contentInverse = Color(0xFFFFFFFF),
                border = Color(0xFFD0DBEE),
                borderStrong = Color(0xFFCBD1DB),
                divider = Color(0xFFDAE2F2),
                backgroundGrouped = Color(0xFFE1E8F6),
                surfaceGrouped = Color(0xFFFFFFFF),
                surfaceElevated = Color(0xFFFFFFFF),
                fill = Color(0xFFE9ECF1),
                separator = Color(0xFFE3E6EC),
                highlight = Color(0xFFE9ECF1),
                skeleton = Color(0xFFDAE2F2),
                scrim = Color(0x730B0F1A),
                shadow = Color(0x1F000000),
                focusRing = Color(0x662C6BED),
                accent = Color(0xFF2C6BED),
                accentPressed = Color(0xFF1F52C4),
                accentSubtle = Color(0xFFE8EFFD),
                accentContent = Color(0xFF1C60EC),
                onAccent = Color(0xFFFFFFFF),
                secondary = Color(0xFF617DB8),
                secondaryPressed = Color(0xFF445F97),
                secondarySubtle = Color(0xFFEFF2F8),
                secondaryContent = Color(0xFF4D6BAB),
                onSecondary = Color(0xFF0B0E14),
                tertiary = Color(0xFFAE2CED),
                tertiaryPressed = Color(0xFF8E11CA),
                tertiarySubtle = Color(0xFFF7EAFD),
                tertiaryContent = Color(0xFFA81CEC),
                onTertiary = Color(0xFFFFFFFF),
                success = Status(Color(0xFF117C59), Color(0xFF117C59), Color(0xFFE4F5EE), Color(0xFFB7E2D2), Color(0xFFFFFFFF)),
                warning = Status(Color(0xFFA35B00), Color(0xFFA35B00), Color(0xFFFDF1E0), Color(0xFFF2DDB8), Color(0xFFFFFFFF)),
                danger = Status(Color(0xFFC42B32), Color(0xFFC42B32), Color(0xFFFCE9E9), Color(0xFFF3C9CB), Color(0xFFFFFFFF)),
                info = Status(Color(0xFF1E5FBF), Color(0xFF1E5FBF), Color(0xFFE7F0FD), Color(0xFFC3D8F7), Color(0xFFFFFFFF)),
                neutral = Status(Color(0xFF5A6472), Color(0xFF5A6472), Color(0xFFEFF1F5), Color(0xFFE3E6EC), Color(0xFFFFFFFF)),
            )
            val dark = Roles(
                background = Color(0xFF0F131D),
                surface = Color(0xFF171E2B),
                surfaceVariant = Color(0xFF212A3A),
                surfaceInverse = Color(0xFFF2F4F8),
                contentPrimary = Color(0xFFF2F4F8),
                contentSecondary = Color(0xFFB4BCCA),
                contentTertiary = Color(0xFF7F8899),
                contentDisabled = Color(0xFF565F6E),
                contentInverse = Color(0xFF0B0E14),
                border = Color(0xFF2B364A),
                borderStrong = Color(0xFF38414F),
                divider = Color(0xFF232D3E),
                backgroundGrouped = Color(0xFF0F131D),
                surfaceGrouped = Color(0xFF141922),
                surfaceElevated = Color(0xFF1D232E),
                fill = Color(0xFF1D232E),
                separator = Color(0xFF262D3A),
                highlight = Color(0xFF262D3A),
                skeleton = Color(0xFF222B3B),
                scrim = Color(0x99000000),
                shadow = Color(0x66000000),
                focusRing = Color(0x735B8DEF),
                accent = Color(0xFF5B8DEF),
                accentPressed = Color(0xFF7BA5F5),
                accentSubtle = Color(0xFF16233B),
                accentContent = Color(0xFF5B8DEF),
                onAccent = Color(0xFF06101F),
                secondary = Color(0xFF879BC5),
                secondaryPressed = Color(0xFFA4B3D0),
                secondarySubtle = Color(0xFF171F31),
                secondaryContent = Color(0xFF879BC5),
                onSecondary = Color(0xFF06101F),
                tertiary = Color(0xFFBE60EB),
                tertiaryPressed = Color(0xFFCB89EC),
                tertiarySubtle = Color(0xFF221438),
                tertiaryContent = Color(0xFFBE60EB),
                onTertiary = Color(0xFF06101F),
                success = Status(Color(0xFF3DD4A0), Color(0xFF3DD4A0), Color(0xFF10271F), Color(0xFF1E4638), Color(0xFF06101F)),
                warning = Status(Color(0xFFE9A23B), Color(0xFFE9A23B), Color(0xFF2A1F0E), Color(0xFF4A3616), Color(0xFF06101F)),
                danger = Status(Color(0xFFF0666D), Color(0xFFF0666D), Color(0xFF2B1416), Color(0xFF4E2126), Color(0xFF06101F)),
                info = Status(Color(0xFF6FA6FF), Color(0xFF6FA6FF), Color(0xFF10203A), Color(0xFF1E3862), Color(0xFF06101F)),
                neutral = Status(Color(0xFFB4BCCA), Color(0xFFB4BCCA), Color(0xFF1A202B), Color(0xFF2C3441), Color(0xFF06101F)),
            )
        }

        object Paper {
            val light = Roles(
                background = Color(0xFFF7F4EE),
                surface = Color(0xFFFFFDF9),
                surfaceVariant = Color(0xFFEFEAE1),
                surfaceInverse = Color(0xFF0B0F1A),
                contentPrimary = Color(0xFF15130F),
                contentSecondary = Color(0xFF3F3931),
                contentTertiary = Color(0xFF6A6256),
                contentDisabled = Color(0xFFA3AAB6),
                contentInverse = Color(0xFFFFFFFF),
                border = Color(0xFFE2DBCF),
                borderStrong = Color(0xFFCCC2B1),
                divider = Color(0xFFEAE4DA),
                backgroundGrouped = Color(0xFFEFEAE1),
                surfaceGrouped = Color(0xFFFFFDF9),
                surfaceElevated = Color(0xFFFFFDF9),
                fill = Color(0xFFEAE4DA),
                separator = Color(0xFFE2DBCF),
                highlight = Color(0xFFEAE4DA),
                skeleton = Color(0xFFEAE4DA),
                scrim = Color(0x730B0F1A),
                shadow = Color(0x1F000000),
                focusRing = Color(0x662C6BED),
                accent = Color(0xFF2C6BED),
                accentPressed = Color(0xFF1F52C4),
                accentSubtle = Color(0xFFE8EFFD),
                accentContent = Color(0xFF1C60EC),
                onAccent = Color(0xFFFFFFFF),
                secondary = Color(0xFF617DB8),
                secondaryPressed = Color(0xFF445F97),
                secondarySubtle = Color(0xFFEFF2F8),
                secondaryContent = Color(0xFF4D6BAB),
                onSecondary = Color(0xFF0B0E14),
                tertiary = Color(0xFFAE2CED),
                tertiaryPressed = Color(0xFF8E11CA),
                tertiarySubtle = Color(0xFFF7EAFD),
                tertiaryContent = Color(0xFFA81CEC),
                onTertiary = Color(0xFFFFFFFF),
                success = Status(Color(0xFF117C59), Color(0xFF117C59), Color(0xFFE4F5EE), Color(0xFFB7E2D2), Color(0xFFFFFFFF)),
                warning = Status(Color(0xFFA35B00), Color(0xFFA35B00), Color(0xFFFDF1E0), Color(0xFFF2DDB8), Color(0xFFFFFFFF)),
                danger = Status(Color(0xFFC42B32), Color(0xFFC42B32), Color(0xFFFCE9E9), Color(0xFFF3C9CB), Color(0xFFFFFFFF)),
                info = Status(Color(0xFF1E5FBF), Color(0xFF1E5FBF), Color(0xFFE7F0FD), Color(0xFFC3D8F7), Color(0xFFFFFFFF)),
                neutral = Status(Color(0xFF5A6472), Color(0xFF5A6472), Color(0xFFEFF1F5), Color(0xFFE3E6EC), Color(0xFFFFFFFF)),
            )
            val dark = Roles(
                background = Color(0xFF12110F),
                surface = Color(0xFF1B1916),
                surfaceVariant = Color(0xFF25221E),
                surfaceInverse = Color(0xFFF2F4F8),
                contentPrimary = Color(0xFFF4EFE6),
                contentSecondary = Color(0xFFCBC2B4),
                contentTertiary = Color(0xFF999080),
                contentDisabled = Color(0xFF565F6E),
                contentInverse = Color(0xFF0B0E14),
                border = Color(0xFF34302A),
                borderStrong = Color(0xFF4A443B),
                divider = Color(0xFF27241F),
                backgroundGrouped = Color(0xFF12110F),
                surfaceGrouped = Color(0xFF1B1916),
                surfaceElevated = Color(0xFF25221E),
                fill = Color(0xFF25221E),
                separator = Color(0xFF34302A),
                highlight = Color(0xFF34302A),
                skeleton = Color(0xFF27241F),
                scrim = Color(0x99000000),
                shadow = Color(0x66000000),
                focusRing = Color(0x735B8DEF),
                accent = Color(0xFF5B8DEF),
                accentPressed = Color(0xFF7BA5F5),
                accentSubtle = Color(0xFF16233B),
                accentContent = Color(0xFF5B8DEF),
                onAccent = Color(0xFF06101F),
                secondary = Color(0xFF879BC5),
                secondaryPressed = Color(0xFFA4B3D0),
                secondarySubtle = Color(0xFF171F31),
                secondaryContent = Color(0xFF879BC5),
                onSecondary = Color(0xFF06101F),
                tertiary = Color(0xFFBE60EB),
                tertiaryPressed = Color(0xFFCB89EC),
                tertiarySubtle = Color(0xFF221438),
                tertiaryContent = Color(0xFFBE60EB),
                onTertiary = Color(0xFF06101F),
                success = Status(Color(0xFF3DD4A0), Color(0xFF3DD4A0), Color(0xFF10271F), Color(0xFF1E4638), Color(0xFF06101F)),
                warning = Status(Color(0xFFE9A23B), Color(0xFFE9A23B), Color(0xFF2A1F0E), Color(0xFF4A3616), Color(0xFF06101F)),
                danger = Status(Color(0xFFF0666D), Color(0xFFF0666D), Color(0xFF2B1416), Color(0xFF4E2126), Color(0xFF06101F)),
                info = Status(Color(0xFF6FA6FF), Color(0xFF6FA6FF), Color(0xFF10203A), Color(0xFF1E3862), Color(0xFF06101F)),
                neutral = Status(Color(0xFFB4BCCA), Color(0xFFB4BCCA), Color(0xFF1A202B), Color(0xFF2C3441), Color(0xFF06101F)),
            )
        }

        object Cream {
            val light = Roles(
                background = Color(0xFFFFF7EA),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFFFEFD6),
                surfaceInverse = Color(0xFF0B0F1A),
                contentPrimary = Color(0xFF1A1206),
                contentSecondary = Color(0xFF45392A),
                contentTertiary = Color(0xFF6F6048),
                contentDisabled = Color(0xFFA3AAB6),
                contentInverse = Color(0xFFFFFFFF),
                border = Color(0xFFF0DFC2),
                borderStrong = Color(0xFFDDC59D),
                divider = Color(0xFFF6E8D0),
                backgroundGrouped = Color(0xFFFFEFD6),
                surfaceGrouped = Color(0xFFFFFFFF),
                surfaceElevated = Color(0xFFFFFFFF),
                fill = Color(0xFFF6E8D0),
                separator = Color(0xFFF0DFC2),
                highlight = Color(0xFFF6E8D0),
                skeleton = Color(0xFFF6E8D0),
                scrim = Color(0x730B0F1A),
                shadow = Color(0x1F000000),
                focusRing = Color(0x662C6BED),
                accent = Color(0xFF2C6BED),
                accentPressed = Color(0xFF1F52C4),
                accentSubtle = Color(0xFFE8EFFD),
                accentContent = Color(0xFF1C60EC),
                onAccent = Color(0xFFFFFFFF),
                secondary = Color(0xFF617DB8),
                secondaryPressed = Color(0xFF445F97),
                secondarySubtle = Color(0xFFEFF2F8),
                secondaryContent = Color(0xFF4D6BAB),
                onSecondary = Color(0xFF0B0E14),
                tertiary = Color(0xFFAE2CED),
                tertiaryPressed = Color(0xFF8E11CA),
                tertiarySubtle = Color(0xFFF7EAFD),
                tertiaryContent = Color(0xFFAE2CED),
                onTertiary = Color(0xFFFFFFFF),
                success = Status(Color(0xFF117C59), Color(0xFF117C59), Color(0xFFE4F5EE), Color(0xFFB7E2D2), Color(0xFFFFFFFF)),
                warning = Status(Color(0xFFA35B00), Color(0xFFA35B00), Color(0xFFFDF1E0), Color(0xFFF2DDB8), Color(0xFFFFFFFF)),
                danger = Status(Color(0xFFC42B32), Color(0xFFC42B32), Color(0xFFFCE9E9), Color(0xFFF3C9CB), Color(0xFFFFFFFF)),
                info = Status(Color(0xFF1E5FBF), Color(0xFF1E5FBF), Color(0xFFE7F0FD), Color(0xFFC3D8F7), Color(0xFFFFFFFF)),
                neutral = Status(Color(0xFF5A6472), Color(0xFF5A6472), Color(0xFFEFF1F5), Color(0xFFE3E6EC), Color(0xFFFFFFFF)),
            )
            val dark = Roles(
                background = Color(0xFF15120D),
                surface = Color(0xFF1F1A13),
                surfaceVariant = Color(0xFF2A2419),
                surfaceInverse = Color(0xFFF2F4F8),
                contentPrimary = Color(0xFFFBF3E4),
                contentSecondary = Color(0xFFD6C8B0),
                contentTertiary = Color(0xFFA3947B),
                contentDisabled = Color(0xFF565F6E),
                contentInverse = Color(0xFF0B0E14),
                border = Color(0xFF3A3224),
                borderStrong = Color(0xFF514634),
                divider = Color(0xFF2B2519),
                backgroundGrouped = Color(0xFF15120D),
                surfaceGrouped = Color(0xFF1F1A13),
                surfaceElevated = Color(0xFF2A2419),
                fill = Color(0xFF2A2419),
                separator = Color(0xFF3A3224),
                highlight = Color(0xFF3A3224),
                skeleton = Color(0xFF2B2519),
                scrim = Color(0x99000000),
                shadow = Color(0x66000000),
                focusRing = Color(0x735B8DEF),
                accent = Color(0xFF5B8DEF),
                accentPressed = Color(0xFF7BA5F5),
                accentSubtle = Color(0xFF16233B),
                accentContent = Color(0xFF5B8DEF),
                onAccent = Color(0xFF06101F),
                secondary = Color(0xFF879BC5),
                secondaryPressed = Color(0xFFA4B3D0),
                secondarySubtle = Color(0xFF171F31),
                secondaryContent = Color(0xFF879BC5),
                onSecondary = Color(0xFF06101F),
                tertiary = Color(0xFFBE60EB),
                tertiaryPressed = Color(0xFFCB89EC),
                tertiarySubtle = Color(0xFF221438),
                tertiaryContent = Color(0xFFBE60EB),
                onTertiary = Color(0xFF06101F),
                success = Status(Color(0xFF3DD4A0), Color(0xFF3DD4A0), Color(0xFF10271F), Color(0xFF1E4638), Color(0xFF06101F)),
                warning = Status(Color(0xFFE9A23B), Color(0xFFE9A23B), Color(0xFF2A1F0E), Color(0xFF4A3616), Color(0xFF06101F)),
                danger = Status(Color(0xFFF0666D), Color(0xFFF0666D), Color(0xFF2B1416), Color(0xFF4E2126), Color(0xFF06101F)),
                info = Status(Color(0xFF6FA6FF), Color(0xFF6FA6FF), Color(0xFF10203A), Color(0xFF1E3862), Color(0xFF06101F)),
                neutral = Status(Color(0xFFB4BCCA), Color(0xFFB4BCCA), Color(0xFF1A202B), Color(0xFF2C3441), Color(0xFF06101F)),
            )
        }
    }

    class Style(
        val radiusXs: Dp,
        val radiusSm: Dp,
        val radiusMd: Dp,
        val radiusLg: Dp,
        val radiusXl: Dp,
        val radiusSheet: Dp,
        /** Null: a pill, as round as the component is tall. */
        val buttonRadius: Dp?,
        val chipRadius: Dp?,
        val card: CardTreatment,
        val field: FieldTreatment,
        val bar: BarTreatment,
        val borderWidth: Dp,
        val borderWidthStrong: Dp,
        val uppercaseLabels: Boolean,
        val offsetShadow: Dp,
        val surfaces: SurfaceTone,
        val voice: TypeVoice,
    )

    object Styles {
        val utility = Style(
            radiusXs = 6.dp,
            radiusSm = 10.dp,
            radiusMd = 14.dp,
            radiusLg = 20.dp,
            radiusXl = 28.dp,
            radiusSheet = 24.dp,
            buttonRadius = 10.dp,
            chipRadius = null,
            card = CardTreatment.Outlined,
            field = FieldTreatment.Outlined,
            bar = BarTreatment.Docked,
            borderWidth = 1.dp,
            borderWidthStrong = 1.5.dp,
            uppercaseLabels = false,
            offsetShadow = 0.dp,
            surfaces = SurfaceTone.Cool,
            voice = TypeVoice.Neutral,
        )
        val social = Style(
            radiusXs = 10.dp,
            radiusSm = 14.dp,
            radiusMd = 20.dp,
            radiusLg = 26.dp,
            radiusXl = 32.dp,
            radiusSheet = 30.dp,
            buttonRadius = null,
            chipRadius = null,
            card = CardTreatment.Raised,
            field = FieldTreatment.Filled,
            bar = BarTreatment.Floating,
            borderWidth = 1.dp,
            borderWidthStrong = 1.5.dp,
            uppercaseLabels = false,
            offsetShadow = 0.dp,
            surfaces = SurfaceTone.BrandTint,
            voice = TypeVoice.Bold,
        )
        val editorial = Style(
            radiusXs = 2.dp,
            radiusSm = 3.dp,
            radiusMd = 4.dp,
            radiusLg = 6.dp,
            radiusXl = 8.dp,
            radiusSheet = 8.dp,
            buttonRadius = 2.dp,
            chipRadius = 2.dp,
            card = CardTreatment.Outlined,
            field = FieldTreatment.Underlined,
            bar = BarTreatment.Minimal,
            borderWidth = 1.dp,
            borderWidthStrong = 1.5.dp,
            uppercaseLabels = true,
            offsetShadow = 0.dp,
            surfaces = SurfaceTone.Paper,
            voice = TypeVoice.Editorial,
        )
        val playful = Style(
            radiusXs = 8.dp,
            radiusSm = 12.dp,
            radiusMd = 16.dp,
            radiusLg = 22.dp,
            radiusXl = 28.dp,
            radiusSheet = 28.dp,
            buttonRadius = 14.dp,
            chipRadius = 12.dp,
            card = CardTreatment.Offset,
            field = FieldTreatment.Outlined,
            bar = BarTreatment.Chunky,
            borderWidth = 2.dp,
            borderWidthStrong = 2.5.dp,
            uppercaseLabels = false,
            offsetShadow = 4.dp,
            surfaces = SurfaceTone.Cream,
            voice = TypeVoice.Rounded,
        )
    }

    /** The project's own choices, which `AppTheme` falls back to. */
    object Defaults {
        val designStyle: AppDesignStyle = AppDesignStyle.Utility
        val motionStyle: AppMotionStyle = AppMotionStyle.Standard
        const val hapticsEnabled: Boolean = true
        const val fontName: String = "DM Sans"
        const val monoFontName: String = "JetBrains Mono"
    }
}
