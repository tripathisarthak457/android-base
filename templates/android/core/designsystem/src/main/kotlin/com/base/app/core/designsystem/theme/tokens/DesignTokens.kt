// Generated from tokens/*.json by generator/genkit/tokens. Do not edit; run python3 -m genkit.tokens --write.
package com.base.app.core.designsystem.theme.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.base.app.core.designsystem.theme.AppColors
import com.base.app.core.designsystem.theme.AppDesignStyle
import com.base.app.core.designsystem.theme.AppMotionStyle
import com.base.app.core.designsystem.theme.BarIndicator
import com.base.app.core.designsystem.theme.BarTreatment
import com.base.app.core.designsystem.theme.CardTreatment
import com.base.app.core.designsystem.theme.FieldTreatment
import com.base.app.core.designsystem.theme.StatusColors
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
        val buttonSmall: Dp = 32.dp
        val buttonMedium: Dp = 44.dp
        val buttonLarge: Dp = 52.dp
        val fieldHeight: Dp = 48.dp
        val rowHeight: Dp = 48.dp
        val topBarHeight: Dp = 52.dp
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
        val largeTitle = TypeRole(34.sp, 41.sp, (-0.6).sp, 700, 800, 500, 900)
        val title1 = TypeRole(28.sp, 34.sp, (-0.5).sp, 700, 800, 500, 900)
        val title2 = TypeRole(22.sp, 28.sp, (-0.35).sp, 700, 800, 500, 800)
        val title3 = TypeRole(20.sp, 25.sp, (-0.25).sp, 600, 700, 600, 800)
        val headline = TypeRole(17.sp, 22.sp, (-0.15).sp, 600, 700, 600, 700)
        val body = TypeRole(17.sp, 22.sp, (-0.15).sp, 400, 400, 400, 400)
        val callout = TypeRole(16.sp, 21.sp, (-0.1).sp, 400, 400, 400, 400)
        val subheadline = TypeRole(15.sp, 20.sp, (-0.05).sp, 400, 400, 400, 400)
        val footnote = TypeRole(13.sp, 18.sp, 0.sp, 400, 400, 400, 400)
        val caption1 = TypeRole(12.sp, 16.sp, 0.05.sp, 400, 400, 400, 400)
        val caption2 = TypeRole(11.sp, 13.sp, 0.1.sp, 500, 500, 500, 600)
        val mono = TypeRole(15.sp, 20.sp, 0.sp, 400, 400, 400, 400)
    }

    /** A spring in iOS's terms: seconds to settle, and how far it overshoots (negative: overdamped). */
    class Spring(val response: Float, val bounce: Float)

    class MotionStyle(
        val summary: String,
        val press: Spring,
        val release: Spring,
        val thumb: Spring,
        val indicatorLead: Spring,
        val indicatorTrail: Spring,
        val screen: Spring,
        val sheet: Spring,
        val dialog: Spring,
        val layout: Spring,
        val emphasis: Spring,
        val interactive: Spring,
        val bounceBack: Spring,
        val iconBounceDown: Spring,
        val iconBounceUp: Spring,
    )

    object Motion {
        val standard = MotionStyle(
            summary = "Crisp, a trace of overshoot. Lively without asking for attention.",
            press = Spring(0.16f, 0.0f),
            release = Spring(0.32f, 0.15f),
            thumb = Spring(0.28f, 0.0f),
            indicatorLead = Spring(0.22f, 0.15f),
            indicatorTrail = Spring(0.32f, 0.0f),
            screen = Spring(0.344f, 0.0f),
            sheet = Spring(0.34f, 0.0f),
            dialog = Spring(0.275f, 0.0f),
            layout = Spring(0.36f, 0.0f),
            emphasis = Spring(0.5f, 0.15f),
            interactive = Spring(0.15f, 0.14f),
            bounceBack = Spring(0.444f, -0.1f),
            iconBounceDown = Spring(0.1622f, 0.0f),
            iconBounceUp = Spring(0.281f, 0.55f),
        )
        val bouncy = MotionStyle(
            summary = "Springy, with a visible overshoot. Playful; wrong for anything financial.",
            press = Spring(0.16f, 0.0f),
            release = Spring(0.32f, 0.35f),
            thumb = Spring(0.28f, 0.0f),
            indicatorLead = Spring(0.22f, 0.35f),
            indicatorTrail = Spring(0.32f, 0.0f),
            screen = Spring(0.344f, 0.0f),
            sheet = Spring(0.34f, 0.0f),
            dialog = Spring(0.275f, 0.0f),
            layout = Spring(0.36f, 0.0f),
            emphasis = Spring(0.5f, 0.35f),
            interactive = Spring(0.15f, 0.14f),
            bounceBack = Spring(0.444f, -0.1f),
            iconBounceDown = Spring(0.1622f, 0.0f),
            iconBounceUp = Spring(0.281f, 0.55f),
        )
        val calm = MotionStyle(
            summary = "No overshoot anywhere, slightly longer. For dense, professional interfaces.",
            press = Spring(0.16f, 0.0f),
            release = Spring(0.32f, 0.0f),
            thumb = Spring(0.28f, 0.0f),
            indicatorLead = Spring(0.22f, 0.0f),
            indicatorTrail = Spring(0.32f, 0.0f),
            screen = Spring(0.3954f, 0.0f),
            sheet = Spring(0.3908f, 0.0f),
            dialog = Spring(0.3161f, 0.0f),
            layout = Spring(0.4138f, 0.0f),
            emphasis = Spring(0.5f, 0.0f),
            interactive = Spring(0.15f, 0.0f),
            bounceBack = Spring(0.444f, -0.1f),
            iconBounceDown = Spring(0.1622f, 0.0f),
            iconBounceUp = Spring(0.281f, 0.0f),
        )
        val snappy = MotionStyle(
            summary = "The shortest response that still reads as motion. For utilities.",
            press = Spring(0.16f, 0.0f),
            release = Spring(0.32f, 0.1f),
            thumb = Spring(0.28f, 0.0f),
            indicatorLead = Spring(0.22f, 0.1f),
            indicatorTrail = Spring(0.32f, 0.0f),
            screen = Spring(0.2991f, 0.0f),
            sheet = Spring(0.2957f, 0.0f),
            dialog = Spring(0.2391f, 0.0f),
            layout = Spring(0.313f, 0.0f),
            emphasis = Spring(0.5f, 0.1f),
            interactive = Spring(0.15f, 0.14f),
            bounceBack = Spring(0.444f, -0.1f),
            iconBounceDown = Spring(0.1622f, 0.0f),
            iconBounceUp = Spring(0.281f, 0.55f),
        )

        const val rubberBand: Float = 0.55f
        const val flingFriction: Float = 0.4767f
        const val pagingFriction: Float = 2.393f
        const val overscrollReleaseStiffness: Float = 9800000.0f
        const val overscrollReleaseDampingRatio: Float = 158.1f
        const val visibilityThreshold: Float = 0.001f
        const val offsetVisibilityThresholdPx: Int = 1
        const val fadeShortMs: Int = 120
        const val fadeLongMs: Int = 200
        val fadeEnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
        val fadeExitEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
        const val staggerStepMs: Int = 24
        const val staggerMaxItems: Int = 6
        val staggerOffset: Dp = 12.dp
        const val pressScale: Float = 0.96f
        const val pressScaleLarge: Float = 0.98f
        const val pressScaleIcon: Float = 0.9f
        const val pressDimAlpha: Float = 0.45f
        const val highlightFadeOutMs: Int = 220
        const val navigationParallax: Float = 0.333f
        const val navigationScrimAlphaLight: Float = 0.1f
        const val navigationScrimAlphaDark: Float = 0.24f
        val navigationEdgeShadow: Dp = 8.dp
        const val navigationEdgeShadowAlpha: Float = 0.04f
        const val navigationBackGateRelease: Float = 0.99f
        val tabRevealShift: Dp = 12.dp
        const val tabRevealVeilAlpha: Float = 0.6f
        val tabIndicatorHeight: Dp = 44.dp
        const val tabIndicatorMaxStretch: Float = 1.5f
        const val tabIndicatorSquash: Float = 0.06f
        const val tabIconBounceScale: Float = 0.84f
        val tabMinimizeHysteresis: Dp = 10.dp
        val skeletonBandWidth: Dp = 240.dp
        const val skeletonTiltDegrees: Float = 20.0f
        const val skeletonSweepMs: Int = 1200
        const val skeletonPauseMs: Int = 800
        val skeletonEasing = CubicBezierEasing(0.3f, 0.0f, 0.7f, 1.0f)
        val skeletonReferenceTravel: Dp = 900.dp
        const val skeletonMaxSlowdown: Float = 2.0f
    }

    object Palette {
        object Cool {
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
                barIndicator = Color(0xFF2C6BED),
                skeleton = Color(0xFFD7DADF),
                skeletonHighlight = Color(0xFFE5E8ED),
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
                barIndicator = Color(0xFF5B8DEF),
                skeleton = Color(0xFF2D333E),
                skeletonHighlight = Color(0xFF393F4B),
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

        object BrandTint {
            val light = AppColors(
                background = Color(0xFFEEF2FA),
                backgroundGrouped = Color(0xFFE8EDF7),
                surface = Color(0xFFFFFFFF),
                surfaceGrouped = Color(0xFFFFFFFF),
                surfaceElevated = Color(0xFFFFFFFF),
                surfaceInverse = Color(0xFF0B0F1A),
                fill = Color(0xFFDBE4F2),
                segmentThumb = Color(0xFFFFFFFF),
                separator = Color(0xFFD0DBEE),
                outline = Color(0xFF7A818F),
                contentPrimary = Color(0xFF0B0F1A),
                contentSecondary = Color(0xFF3D4552),
                contentTertiary = Color(0xFF626A78),
                contentDisabled = Color(0xFFA3AAB6),
                contentInverse = Color(0xFFFFFFFF),
                highlight = Color(0x120B0F1A),
                scrim = Color(0x660B0F1A),
                focusRing = Color(0xFF2C6BED),
                shadow = Color(0x240B0F1A),
                barIndicator = Color(0xFF2C6BED),
                skeleton = Color(0xFFC9D2E1),
                skeletonHighlight = Color(0xFFD6DFEE),
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
                success = StatusColors(base = Color(0xFF117C59), content = Color(0xFF0F7353), subtle = Color(0xFFE4F5EE), on = Color(0xFFFFFFFF)),
                warning = StatusColors(base = Color(0xFFA35B00), content = Color(0xFF985400), subtle = Color(0xFFFDF1E0), on = Color(0xFFFFFFFF)),
                danger = StatusColors(base = Color(0xFFC42B32), content = Color(0xFFC42B32), subtle = Color(0xFFFCE9E9), on = Color(0xFFFFFFFF)),
                info = StatusColors(base = Color(0xFF1E5FBF), content = Color(0xFF1E5FBF), subtle = Color(0xFFE7F0FD), on = Color(0xFFFFFFFF)),
                neutral = StatusColors(base = Color(0xFF5A6472), content = Color(0xFF5A6472), subtle = Color(0xFFEFF1F5), on = Color(0xFFFFFFFF)),
                isLight = true,
            )
            val dark = AppColors(
                background = Color(0xFF0E131C),
                backgroundGrouped = Color(0xFF0F131D),
                surface = Color(0xFF171E2B),
                surfaceGrouped = Color(0xFF171E2B),
                surfaceElevated = Color(0xFF202836),
                surfaceInverse = Color(0xFFF2F4F8),
                fill = Color(0xFF2A3345),
                segmentThumb = Color(0xFF49515F),
                separator = Color(0xFF2B364A),
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
                barIndicator = Color(0xFF5B8DEF),
                skeleton = Color(0xFF313A4B),
                skeletonHighlight = Color(0xFF3C4557),
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

        object Paper {
            val light = AppColors(
                background = Color(0xFFF7F4EE),
                backgroundGrouped = Color(0xFFF1EDE5),
                surface = Color(0xFFFFFDF9),
                surfaceGrouped = Color(0xFFFFFDF9),
                surfaceElevated = Color(0xFFFFFDF9),
                surfaceInverse = Color(0xFF0B0F1A),
                fill = Color(0xFFEAE4DA),
                segmentThumb = Color(0xFFFFFFFF),
                separator = Color(0xFFE2DBCF),
                outline = Color(0xFF8C8273),
                contentPrimary = Color(0xFF15130F),
                contentSecondary = Color(0xFF3F3931),
                contentTertiary = Color(0xFF655D51),
                contentDisabled = Color(0xFFA3AAB6),
                contentInverse = Color(0xFFFFFFFF),
                highlight = Color(0x120B0F1A),
                scrim = Color(0x660B0F1A),
                focusRing = Color(0xFF2C6BED),
                shadow = Color(0x240B0F1A),
                barIndicator = Color(0xFF2C6BED),
                skeleton = Color(0xFFDAD4CA),
                skeletonHighlight = Color(0xFFE8E2D8),
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
                success = StatusColors(base = Color(0xFF117C59), content = Color(0xFF0F7353), subtle = Color(0xFFE4F5EE), on = Color(0xFFFFFFFF)),
                warning = StatusColors(base = Color(0xFFA35B00), content = Color(0xFF985400), subtle = Color(0xFFFDF1E0), on = Color(0xFFFFFFFF)),
                danger = StatusColors(base = Color(0xFFC42B32), content = Color(0xFFC42B32), subtle = Color(0xFFFCE9E9), on = Color(0xFFFFFFFF)),
                info = StatusColors(base = Color(0xFF1E5FBF), content = Color(0xFF1E5FBF), subtle = Color(0xFFE7F0FD), on = Color(0xFFFFFFFF)),
                neutral = StatusColors(base = Color(0xFF5A6472), content = Color(0xFF5A6472), subtle = Color(0xFFEFF1F5), on = Color(0xFFFFFFFF)),
                isLight = true,
            )
            val dark = AppColors(
                background = Color(0xFF12110F),
                backgroundGrouped = Color(0xFF12110F),
                surface = Color(0xFF1B1916),
                surfaceGrouped = Color(0xFF1B1916),
                surfaceElevated = Color(0xFF25221E),
                surfaceInverse = Color(0xFFF2F4F8),
                fill = Color(0xFF2E2A25),
                segmentThumb = Color(0xFF414957),
                separator = Color(0xFF34302A),
                outline = Color(0xFF7A7164),
                contentPrimary = Color(0xFFF4EFE6),
                contentSecondary = Color(0xFFCBC2B4),
                contentTertiary = Color(0xFFA0978A),
                contentDisabled = Color(0xFF565F6E),
                contentInverse = Color(0xFF0B0E14),
                highlight = Color(0x17FFFFFF),
                scrim = Color(0x8F000000),
                focusRing = Color(0xFF5B8DEF),
                shadow = Color(0x66000000),
                barIndicator = Color(0xFF5B8DEF),
                skeleton = Color(0xFF37332E),
                skeletonHighlight = Color(0xFF443F3A),
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

        object Cream {
            val light = AppColors(
                background = Color(0xFFFFF7EA),
                backgroundGrouped = Color(0xFFFCF1DE),
                surface = Color(0xFFFFFFFF),
                surfaceGrouped = Color(0xFFFFFFFF),
                surfaceElevated = Color(0xFFFFFFFF),
                surfaceInverse = Color(0xFF0B0F1A),
                fill = Color(0xFFF6E8D0),
                segmentThumb = Color(0xFFFFFFFF),
                separator = Color(0xFFF0DFC2),
                outline = Color(0xFF94855F),
                contentPrimary = Color(0xFF1A1206),
                contentSecondary = Color(0xFF45392A),
                contentTertiary = Color(0xFF6A5B43),
                contentDisabled = Color(0xFFA3AAB6),
                contentInverse = Color(0xFFFFFFFF),
                highlight = Color(0x120B0F1A),
                scrim = Color(0x660B0F1A),
                focusRing = Color(0xFF2C6BED),
                shadow = Color(0x240B0F1A),
                barIndicator = Color(0xFF2C6BED),
                skeleton = Color(0xFFE6D8C1),
                skeletonHighlight = Color(0xFFF5E7CF),
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
                background = Color(0xFF15120D),
                backgroundGrouped = Color(0xFF15120D),
                surface = Color(0xFF1F1A13),
                surfaceGrouped = Color(0xFF1F1A13),
                surfaceElevated = Color(0xFF2A2419),
                surfaceInverse = Color(0xFFF2F4F8),
                fill = Color(0xFF332C20),
                segmentThumb = Color(0xFF414957),
                separator = Color(0xFF3A3224),
                outline = Color(0xFF7E715A),
                contentPrimary = Color(0xFFFBF3E4),
                contentSecondary = Color(0xFFD6C8B0),
                contentTertiary = Color(0xFFA99A80),
                contentDisabled = Color(0xFF565F6E),
                contentInverse = Color(0xFF0B0E14),
                highlight = Color(0x17FFFFFF),
                scrim = Color(0x8F000000),
                focusRing = Color(0xFF5B8DEF),
                shadow = Color(0x66000000),
                barIndicator = Color(0xFF5B8DEF),
                skeleton = Color(0xFF3B3427),
                skeletonHighlight = Color(0xFF484033),
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
        val indicator: BarIndicator,
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
            indicator = BarIndicator.Subtle,
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
            indicator = BarIndicator.Pill,
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
            bar = BarTreatment.Translucent,
            indicator = BarIndicator.Subtle,
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
            bar = BarTreatment.Floating,
            indicator = BarIndicator.Pill,
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
