// Generated from tokens/*.json by generator/genkit/tokens. Do not edit; run python3 -m genkit.tokens --write.
import SwiftUI

/// A design style: corner radii, container treatments, surface tones and type voice.
public nonisolated enum AppDesignStyle: String, CaseIterable, Sendable {
    /// Cool neutrals, hairline outlines, a docked tab bar. Tools, finance, admin.
    case utility = "Utility"

    /// Brand-tinted surfaces, pill buttons, heavier headlines, a floating tab bar. Feeds and chat.
    case social = "Social"

    /// Warm paper and ink, large tight display type, underlined fields. Reading and news.
    case editorial = "Editorial"

    /// Cream surfaces, extra-bold type, thick outlines and offset shadows. Games and kids.
    case playful = "Playful"
}

/// A motion style: how much the springs overshoot, and how quickly they travel.
public nonisolated enum AppMotionStyle: String, CaseIterable, Sendable {
    /// Crisp, a trace of overshoot. Lively without asking for attention.
    case standard = "Standard"

    /// Springy, with a visible overshoot. Playful; wrong for anything financial.
    case bouncy = "Bouncy"

    /// No overshoot, slightly longer. For dense, professional interfaces.
    case calm = "Calm"

    /// The shortest response that still reads as motion. For utilities.
    case snappy = "Snappy"
}

nonisolated enum DesignTokens {
    nonisolated enum Spacing {
        static let xxs: CGFloat = 2
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 12
        static let lg: CGFloat = 16
        static let xl: CGFloat = 24
        static let xxl: CGFloat = 32
        static let xxxl: CGFloat = 48
        static let gutter: CGFloat = 16
        static let section: CGFloat = 24
        static let stack: CGFloat = 12
    }

    nonisolated enum Radii {
        static let none: CGFloat = 0
        static let xs: CGFloat = 6
        static let sm: CGFloat = 10
        static let md: CGFloat = 14
        static let lg: CGFloat = 20
        static let xl: CGFloat = 28
        static let sheet: CGFloat = 24
    }

    nonisolated enum Sizes {
        static let minTouchTarget: CGFloat = 44
        static let iconSmall: CGFloat = 16
        static let icon: CGFloat = 20
        static let iconLarge: CGFloat = 24
        static let buttonSmall: CGFloat = 36
        static let buttonMedium: CGFloat = 44
        static let buttonLarge: CGFloat = 52
        static let fieldHeight: CGFloat = 52
        static let topBarHeight: CGFloat = 56
        static let tabBarHeight: CGFloat = 64
        static let tabBarSideMargin: CGFloat = 20
        static let tabBarLift: CGFloat = 12
        static let tabBarMaxWidth: CGFloat = 480
    }

    nonisolated enum Borders {
        /// Physical pixels, not points: a hairline is `hairlinePx / displayScale` points on every screen.
        static let hairlinePx: Int = 1
        static let width: CGFloat = 1
        static let widthStrong: CGFloat = 1.5
    }

    nonisolated enum Elevation {
        static let none: CGFloat = 0
        static let raised: CGFloat = 1
        static let card: CGFloat = 3
        static let overlay: CGFloat = 8
        static let modal: CGFloat = 16
    }

    nonisolated struct TypeRole {
        let size: CGFloat
        let lineHeight: CGFloat
        let tracking: CGFloat
        let weightNeutral: Int
        let weightBold: Int
        let weightEditorial: Int
        let weightRounded: Int
    }

    nonisolated enum Typography {
        static let largeTitle = TypeRole(size: 34, lineHeight: 41, tracking: -0.8, weightNeutral: 700, weightBold: 900, weightEditorial: 600, weightRounded: 900)
        static let title1 = TypeRole(size: 28, lineHeight: 34, tracking: -0.6, weightNeutral: 700, weightBold: 900, weightEditorial: 600, weightRounded: 900)
        static let title2 = TypeRole(size: 22, lineHeight: 28, tracking: -0.5, weightNeutral: 700, weightBold: 800, weightEditorial: 600, weightRounded: 900)
        static let title3 = TypeRole(size: 20, lineHeight: 25, tracking: -0.3, weightNeutral: 600, weightBold: 800, weightEditorial: 600, weightRounded: 800)
        static let headline = TypeRole(size: 17, lineHeight: 22, tracking: -0.2, weightNeutral: 600, weightBold: 800, weightEditorial: 600, weightRounded: 800)
        static let body = TypeRole(size: 17, lineHeight: 22, tracking: 0, weightNeutral: 400, weightBold: 400, weightEditorial: 400, weightRounded: 400)
        static let callout = TypeRole(size: 16, lineHeight: 21, tracking: 0, weightNeutral: 400, weightBold: 400, weightEditorial: 400, weightRounded: 400)
        static let subheadline = TypeRole(size: 15, lineHeight: 20, tracking: 0, weightNeutral: 400, weightBold: 400, weightEditorial: 400, weightRounded: 400)
        static let footnote = TypeRole(size: 13, lineHeight: 18, tracking: 0, weightNeutral: 400, weightBold: 500, weightEditorial: 400, weightRounded: 500)
        static let caption1 = TypeRole(size: 12, lineHeight: 16, tracking: 0.1, weightNeutral: 400, weightBold: 500, weightEditorial: 400, weightRounded: 500)
        static let caption2 = TypeRole(size: 11, lineHeight: 13, tracking: 0.2, weightNeutral: 500, weightBold: 600, weightEditorial: 500, weightRounded: 600)
        static let mono = TypeRole(size: 15, lineHeight: 20, tracking: 0, weightNeutral: 400, weightBold: 400, weightEditorial: 400, weightRounded: 400)
    }

    /// A spring as response and damping ratio, the pair the Android template's springs use, so
    /// both apps run the same curve whatever the bounce.
    nonisolated struct SpringToken {
        let response: Double
        let dampingRatio: Double

        var spring: Spring {
            Spring(response: response, dampingRatio: dampingRatio)
        }
    }

    nonisolated struct MotionStyle {
        let press: SpringToken
        let release: SpringToken
        let selection: SpringToken
        let screen: SpringToken
        let sheet: SpringToken
        let layout: SpringToken
        let emphasis: SpringToken
    }

    nonisolated enum Motion {
        static let standard = MotionStyle(
            press: SpringToken(response: 0.16, dampingRatio: 1),
            release: SpringToken(response: 0.32, dampingRatio: 0.85),
            selection: SpringToken(response: 0.3, dampingRatio: 0.91),
            screen: SpringToken(response: 0.24, dampingRatio: 1),
            sheet: SpringToken(response: 0.34, dampingRatio: 1),
            layout: SpringToken(response: 0.36, dampingRatio: 1),
            emphasis: SpringToken(response: 0.5, dampingRatio: 0.85)
        )
        static let bouncy = MotionStyle(
            press: SpringToken(response: 0.16, dampingRatio: 1),
            release: SpringToken(response: 0.32, dampingRatio: 0.65),
            selection: SpringToken(response: 0.3, dampingRatio: 0.79),
            screen: SpringToken(response: 0.24, dampingRatio: 1),
            sheet: SpringToken(response: 0.34, dampingRatio: 1),
            layout: SpringToken(response: 0.36, dampingRatio: 1),
            emphasis: SpringToken(response: 0.5, dampingRatio: 0.65)
        )
        static let calm = MotionStyle(
            press: SpringToken(response: 0.16, dampingRatio: 1),
            release: SpringToken(response: 0.32, dampingRatio: 1),
            selection: SpringToken(response: 0.3, dampingRatio: 1),
            screen: SpringToken(response: 0.2759, dampingRatio: 1),
            sheet: SpringToken(response: 0.3908, dampingRatio: 1),
            layout: SpringToken(response: 0.4138, dampingRatio: 1),
            emphasis: SpringToken(response: 0.5, dampingRatio: 1)
        )
        static let snappy = MotionStyle(
            press: SpringToken(response: 0.16, dampingRatio: 1),
            release: SpringToken(response: 0.32, dampingRatio: 0.9),
            selection: SpringToken(response: 0.3, dampingRatio: 0.94),
            screen: SpringToken(response: 0.2087, dampingRatio: 1),
            sheet: SpringToken(response: 0.2957, dampingRatio: 1),
            layout: SpringToken(response: 0.313, dampingRatio: 1),
            emphasis: SpringToken(response: 0.5, dampingRatio: 0.9)
        )

        static func style(_ style: AppMotionStyle) -> MotionStyle {
            switch style {
            case .standard: standard
            case .bouncy: bouncy
            case .calm: calm
            case .snappy: snappy
            }
        }

        static let fadeShort: TimeInterval = 0.12
        static let pressScale: CGFloat = 0.96
        static let pressScaleLarge: CGFloat = 0.98
        static let pressScaleIcon: CGFloat = 0.9
        static let pressDimAlpha: Double = 0.45
        static let highlightFadeOut: TimeInterval = 0.22
    }

    nonisolated struct Status {
        let base: Color
        let content: Color
        let subtle: Color
        let border: Color
        let on: Color
    }

    nonisolated struct Roles {
        let background: Color
        let surface: Color
        let surfaceVariant: Color
        let surfaceInverse: Color
        let contentPrimary: Color
        let contentSecondary: Color
        let contentTertiary: Color
        let contentDisabled: Color
        let contentInverse: Color
        let border: Color
        let borderStrong: Color
        let divider: Color
        let backgroundGrouped: Color
        let surfaceGrouped: Color
        let surfaceElevated: Color
        let fill: Color
        let separator: Color
        let highlight: Color
        let skeleton: Color
        let scrim: Color
        let shadow: Color
        let focusRing: Color
        let accent: Color
        let accentPressed: Color
        let accentSubtle: Color
        let accentContent: Color
        let onAccent: Color
        let secondary: Color
        let secondaryPressed: Color
        let secondarySubtle: Color
        let secondaryContent: Color
        let onSecondary: Color
        let tertiary: Color
        let tertiaryPressed: Color
        let tertiarySubtle: Color
        let tertiaryContent: Color
        let onTertiary: Color
        let success: Status
        let warning: Status
        let danger: Status
        let info: Status
        let neutral: Status
    }

    /// The surface tones a design style is drawn on, resolved per appearance.
    nonisolated enum Palette {
        nonisolated enum Cool {
            static let light = Roles(
                background: Color(.sRGB, red: 0.9686, green: 0.9725, blue: 0.9804, opacity: 1.0000),
                surface: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                surfaceVariant: Color(.sRGB, red: 0.9373, green: 0.9451, blue: 0.9608, opacity: 1.0000),
                surfaceInverse: Color(.sRGB, red: 0.0431, green: 0.0588, blue: 0.1020, opacity: 1.0000),
                contentPrimary: Color(.sRGB, red: 0.0431, green: 0.0588, blue: 0.1020, opacity: 1.0000),
                contentSecondary: Color(.sRGB, red: 0.2392, green: 0.2706, blue: 0.3216, opacity: 1.0000),
                contentTertiary: Color(.sRGB, red: 0.4196, green: 0.4471, blue: 0.5020, opacity: 1.0000),
                contentDisabled: Color(.sRGB, red: 0.6392, green: 0.6667, blue: 0.7137, opacity: 1.0000),
                contentInverse: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                border: Color(.sRGB, red: 0.8902, green: 0.9020, blue: 0.9255, opacity: 1.0000),
                borderStrong: Color(.sRGB, red: 0.7961, green: 0.8196, blue: 0.8588, opacity: 1.0000),
                divider: Color(.sRGB, red: 0.9137, green: 0.9255, blue: 0.9451, opacity: 1.0000),
                backgroundGrouped: Color(.sRGB, red: 0.9373, green: 0.9451, blue: 0.9608, opacity: 1.0000),
                surfaceGrouped: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                surfaceElevated: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                fill: Color(.sRGB, red: 0.9137, green: 0.9255, blue: 0.9451, opacity: 1.0000),
                separator: Color(.sRGB, red: 0.8902, green: 0.9020, blue: 0.9255, opacity: 1.0000),
                highlight: Color(.sRGB, red: 0.9137, green: 0.9255, blue: 0.9451, opacity: 1.0000),
                skeleton: Color(.sRGB, red: 0.9137, green: 0.9255, blue: 0.9451, opacity: 1.0000),
                scrim: Color(.sRGB, red: 0.0431, green: 0.0588, blue: 0.1020, opacity: 0.4510),
                shadow: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.1216),
                focusRing: Color(.sRGB, red: 0.1725, green: 0.4196, blue: 0.9294, opacity: 0.4000),
                accent: Color(.sRGB, red: 0.1725, green: 0.4196, blue: 0.9294, opacity: 1.0000),
                accentPressed: Color(.sRGB, red: 0.1216, green: 0.3216, blue: 0.7686, opacity: 1.0000),
                accentSubtle: Color(.sRGB, red: 0.9098, green: 0.9373, blue: 0.9922, opacity: 1.0000),
                accentContent: Color(.sRGB, red: 0.1098, green: 0.3765, blue: 0.9255, opacity: 1.0000),
                onAccent: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                secondary: Color(.sRGB, red: 0.3804, green: 0.4902, blue: 0.7216, opacity: 1.0000),
                secondaryPressed: Color(.sRGB, red: 0.2667, green: 0.3725, blue: 0.5922, opacity: 1.0000),
                secondarySubtle: Color(.sRGB, red: 0.9373, green: 0.9490, blue: 0.9725, opacity: 1.0000),
                secondaryContent: Color(.sRGB, red: 0.3020, green: 0.4196, blue: 0.6706, opacity: 1.0000),
                onSecondary: Color(.sRGB, red: 0.0431, green: 0.0549, blue: 0.0784, opacity: 1.0000),
                tertiary: Color(.sRGB, red: 0.6824, green: 0.1725, blue: 0.9294, opacity: 1.0000),
                tertiaryPressed: Color(.sRGB, red: 0.5569, green: 0.0667, blue: 0.7922, opacity: 1.0000),
                tertiarySubtle: Color(.sRGB, red: 0.9686, green: 0.9176, blue: 0.9922, opacity: 1.0000),
                tertiaryContent: Color(.sRGB, red: 0.6824, green: 0.1725, blue: 0.9294, opacity: 1.0000),
                onTertiary: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                success: Status(base: Color(.sRGB, red: 0.0667, green: 0.4863, blue: 0.3490, opacity: 1.0000), content: Color(.sRGB, red: 0.0667, green: 0.4863, blue: 0.3490, opacity: 1.0000), subtle: Color(.sRGB, red: 0.8941, green: 0.9608, blue: 0.9333, opacity: 1.0000), border: Color(.sRGB, red: 0.7176, green: 0.8863, blue: 0.8235, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                warning: Status(base: Color(.sRGB, red: 0.6392, green: 0.3569, blue: 0.0000, opacity: 1.0000), content: Color(.sRGB, red: 0.6392, green: 0.3569, blue: 0.0000, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9922, green: 0.9451, blue: 0.8784, opacity: 1.0000), border: Color(.sRGB, red: 0.9490, green: 0.8667, blue: 0.7216, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                danger: Status(base: Color(.sRGB, red: 0.7686, green: 0.1686, blue: 0.1961, opacity: 1.0000), content: Color(.sRGB, red: 0.7686, green: 0.1686, blue: 0.1961, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9882, green: 0.9137, blue: 0.9137, opacity: 1.0000), border: Color(.sRGB, red: 0.9529, green: 0.7882, blue: 0.7961, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                info: Status(base: Color(.sRGB, red: 0.1176, green: 0.3725, blue: 0.7490, opacity: 1.0000), content: Color(.sRGB, red: 0.1176, green: 0.3725, blue: 0.7490, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9059, green: 0.9412, blue: 0.9922, opacity: 1.0000), border: Color(.sRGB, red: 0.7647, green: 0.8471, blue: 0.9686, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                neutral: Status(base: Color(.sRGB, red: 0.3529, green: 0.3922, blue: 0.4471, opacity: 1.0000), content: Color(.sRGB, red: 0.3529, green: 0.3922, blue: 0.4471, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9373, green: 0.9451, blue: 0.9608, opacity: 1.0000), border: Color(.sRGB, red: 0.8902, green: 0.9020, blue: 0.9255, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000))
            )
            static let dark = Roles(
                background: Color(.sRGB, red: 0.0431, green: 0.0549, blue: 0.0784, opacity: 1.0000),
                surface: Color(.sRGB, red: 0.0784, green: 0.0980, blue: 0.1333, opacity: 1.0000),
                surfaceVariant: Color(.sRGB, red: 0.1137, green: 0.1373, blue: 0.1804, opacity: 1.0000),
                surfaceInverse: Color(.sRGB, red: 0.9490, green: 0.9569, blue: 0.9725, opacity: 1.0000),
                contentPrimary: Color(.sRGB, red: 0.9490, green: 0.9569, blue: 0.9725, opacity: 1.0000),
                contentSecondary: Color(.sRGB, red: 0.7059, green: 0.7373, blue: 0.7922, opacity: 1.0000),
                contentTertiary: Color(.sRGB, red: 0.4980, green: 0.5333, blue: 0.6000, opacity: 1.0000),
                contentDisabled: Color(.sRGB, red: 0.3373, green: 0.3725, blue: 0.4314, opacity: 1.0000),
                contentInverse: Color(.sRGB, red: 0.0431, green: 0.0549, blue: 0.0784, opacity: 1.0000),
                border: Color(.sRGB, red: 0.1490, green: 0.1765, blue: 0.2275, opacity: 1.0000),
                borderStrong: Color(.sRGB, red: 0.2196, green: 0.2549, blue: 0.3098, opacity: 1.0000),
                divider: Color(.sRGB, red: 0.1176, green: 0.1451, blue: 0.1922, opacity: 1.0000),
                backgroundGrouped: Color(.sRGB, red: 0.0431, green: 0.0549, blue: 0.0784, opacity: 1.0000),
                surfaceGrouped: Color(.sRGB, red: 0.0784, green: 0.0980, blue: 0.1333, opacity: 1.0000),
                surfaceElevated: Color(.sRGB, red: 0.1137, green: 0.1373, blue: 0.1804, opacity: 1.0000),
                fill: Color(.sRGB, red: 0.1137, green: 0.1373, blue: 0.1804, opacity: 1.0000),
                separator: Color(.sRGB, red: 0.1490, green: 0.1765, blue: 0.2275, opacity: 1.0000),
                highlight: Color(.sRGB, red: 0.1490, green: 0.1765, blue: 0.2275, opacity: 1.0000),
                skeleton: Color(.sRGB, red: 0.1137, green: 0.1373, blue: 0.1804, opacity: 1.0000),
                scrim: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.6000),
                shadow: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.4000),
                focusRing: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 0.4510),
                accent: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 1.0000),
                accentPressed: Color(.sRGB, red: 0.4824, green: 0.6471, blue: 0.9608, opacity: 1.0000),
                accentSubtle: Color(.sRGB, red: 0.0863, green: 0.1373, blue: 0.2314, opacity: 1.0000),
                accentContent: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 1.0000),
                onAccent: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                secondary: Color(.sRGB, red: 0.5294, green: 0.6078, blue: 0.7725, opacity: 1.0000),
                secondaryPressed: Color(.sRGB, red: 0.6431, green: 0.7020, blue: 0.8157, opacity: 1.0000),
                secondarySubtle: Color(.sRGB, red: 0.0902, green: 0.1216, blue: 0.1922, opacity: 1.0000),
                secondaryContent: Color(.sRGB, red: 0.5294, green: 0.6078, blue: 0.7725, opacity: 1.0000),
                onSecondary: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                tertiary: Color(.sRGB, red: 0.7451, green: 0.3765, blue: 0.9216, opacity: 1.0000),
                tertiaryPressed: Color(.sRGB, red: 0.7961, green: 0.5373, blue: 0.9255, opacity: 1.0000),
                tertiarySubtle: Color(.sRGB, red: 0.1333, green: 0.0784, blue: 0.2196, opacity: 1.0000),
                tertiaryContent: Color(.sRGB, red: 0.7451, green: 0.3765, blue: 0.9216, opacity: 1.0000),
                onTertiary: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                success: Status(base: Color(.sRGB, red: 0.2392, green: 0.8314, blue: 0.6275, opacity: 1.0000), content: Color(.sRGB, red: 0.2392, green: 0.8314, blue: 0.6275, opacity: 1.0000), subtle: Color(.sRGB, red: 0.0627, green: 0.1529, blue: 0.1216, opacity: 1.0000), border: Color(.sRGB, red: 0.1176, green: 0.2745, blue: 0.2196, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                warning: Status(base: Color(.sRGB, red: 0.9137, green: 0.6353, blue: 0.2314, opacity: 1.0000), content: Color(.sRGB, red: 0.9137, green: 0.6353, blue: 0.2314, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1647, green: 0.1216, blue: 0.0549, opacity: 1.0000), border: Color(.sRGB, red: 0.2902, green: 0.2118, blue: 0.0863, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                danger: Status(base: Color(.sRGB, red: 0.9412, green: 0.4000, blue: 0.4275, opacity: 1.0000), content: Color(.sRGB, red: 0.9412, green: 0.4000, blue: 0.4275, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1686, green: 0.0784, blue: 0.0863, opacity: 1.0000), border: Color(.sRGB, red: 0.3059, green: 0.1294, blue: 0.1490, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                info: Status(base: Color(.sRGB, red: 0.4353, green: 0.6510, blue: 1.0000, opacity: 1.0000), content: Color(.sRGB, red: 0.4353, green: 0.6510, blue: 1.0000, opacity: 1.0000), subtle: Color(.sRGB, red: 0.0627, green: 0.1255, blue: 0.2275, opacity: 1.0000), border: Color(.sRGB, red: 0.1176, green: 0.2196, blue: 0.3843, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                neutral: Status(base: Color(.sRGB, red: 0.7059, green: 0.7373, blue: 0.7922, opacity: 1.0000), content: Color(.sRGB, red: 0.7059, green: 0.7373, blue: 0.7922, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1020, green: 0.1255, blue: 0.1686, opacity: 1.0000), border: Color(.sRGB, red: 0.1725, green: 0.2039, blue: 0.2549, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000))
            )
        }

        nonisolated enum BrandTint {
            static let light = Roles(
                background: Color(.sRGB, red: 0.9333, green: 0.9490, blue: 0.9804, opacity: 1.0000),
                surface: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                surfaceVariant: Color(.sRGB, red: 0.8824, green: 0.9098, blue: 0.9647, opacity: 1.0000),
                surfaceInverse: Color(.sRGB, red: 0.0431, green: 0.0588, blue: 0.1020, opacity: 1.0000),
                contentPrimary: Color(.sRGB, red: 0.0431, green: 0.0588, blue: 0.1020, opacity: 1.0000),
                contentSecondary: Color(.sRGB, red: 0.2392, green: 0.2706, blue: 0.3216, opacity: 1.0000),
                contentTertiary: Color(.sRGB, red: 0.4196, green: 0.4471, blue: 0.5020, opacity: 1.0000),
                contentDisabled: Color(.sRGB, red: 0.6392, green: 0.6667, blue: 0.7137, opacity: 1.0000),
                contentInverse: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                border: Color(.sRGB, red: 0.8157, green: 0.8588, blue: 0.9333, opacity: 1.0000),
                borderStrong: Color(.sRGB, red: 0.7961, green: 0.8196, blue: 0.8588, opacity: 1.0000),
                divider: Color(.sRGB, red: 0.8549, green: 0.8863, blue: 0.9490, opacity: 1.0000),
                backgroundGrouped: Color(.sRGB, red: 0.8824, green: 0.9098, blue: 0.9647, opacity: 1.0000),
                surfaceGrouped: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                surfaceElevated: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                fill: Color(.sRGB, red: 0.9137, green: 0.9255, blue: 0.9451, opacity: 1.0000),
                separator: Color(.sRGB, red: 0.8902, green: 0.9020, blue: 0.9255, opacity: 1.0000),
                highlight: Color(.sRGB, red: 0.9137, green: 0.9255, blue: 0.9451, opacity: 1.0000),
                skeleton: Color(.sRGB, red: 0.8549, green: 0.8863, blue: 0.9490, opacity: 1.0000),
                scrim: Color(.sRGB, red: 0.0431, green: 0.0588, blue: 0.1020, opacity: 0.4510),
                shadow: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.1216),
                focusRing: Color(.sRGB, red: 0.1725, green: 0.4196, blue: 0.9294, opacity: 0.4000),
                accent: Color(.sRGB, red: 0.1725, green: 0.4196, blue: 0.9294, opacity: 1.0000),
                accentPressed: Color(.sRGB, red: 0.1216, green: 0.3216, blue: 0.7686, opacity: 1.0000),
                accentSubtle: Color(.sRGB, red: 0.9098, green: 0.9373, blue: 0.9922, opacity: 1.0000),
                accentContent: Color(.sRGB, red: 0.1098, green: 0.3765, blue: 0.9255, opacity: 1.0000),
                onAccent: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                secondary: Color(.sRGB, red: 0.3804, green: 0.4902, blue: 0.7216, opacity: 1.0000),
                secondaryPressed: Color(.sRGB, red: 0.2667, green: 0.3725, blue: 0.5922, opacity: 1.0000),
                secondarySubtle: Color(.sRGB, red: 0.9373, green: 0.9490, blue: 0.9725, opacity: 1.0000),
                secondaryContent: Color(.sRGB, red: 0.3020, green: 0.4196, blue: 0.6706, opacity: 1.0000),
                onSecondary: Color(.sRGB, red: 0.0431, green: 0.0549, blue: 0.0784, opacity: 1.0000),
                tertiary: Color(.sRGB, red: 0.6824, green: 0.1725, blue: 0.9294, opacity: 1.0000),
                tertiaryPressed: Color(.sRGB, red: 0.5569, green: 0.0667, blue: 0.7922, opacity: 1.0000),
                tertiarySubtle: Color(.sRGB, red: 0.9686, green: 0.9176, blue: 0.9922, opacity: 1.0000),
                tertiaryContent: Color(.sRGB, red: 0.6588, green: 0.1098, blue: 0.9255, opacity: 1.0000),
                onTertiary: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                success: Status(base: Color(.sRGB, red: 0.0667, green: 0.4863, blue: 0.3490, opacity: 1.0000), content: Color(.sRGB, red: 0.0667, green: 0.4863, blue: 0.3490, opacity: 1.0000), subtle: Color(.sRGB, red: 0.8941, green: 0.9608, blue: 0.9333, opacity: 1.0000), border: Color(.sRGB, red: 0.7176, green: 0.8863, blue: 0.8235, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                warning: Status(base: Color(.sRGB, red: 0.6392, green: 0.3569, blue: 0.0000, opacity: 1.0000), content: Color(.sRGB, red: 0.6392, green: 0.3569, blue: 0.0000, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9922, green: 0.9451, blue: 0.8784, opacity: 1.0000), border: Color(.sRGB, red: 0.9490, green: 0.8667, blue: 0.7216, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                danger: Status(base: Color(.sRGB, red: 0.7686, green: 0.1686, blue: 0.1961, opacity: 1.0000), content: Color(.sRGB, red: 0.7686, green: 0.1686, blue: 0.1961, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9882, green: 0.9137, blue: 0.9137, opacity: 1.0000), border: Color(.sRGB, red: 0.9529, green: 0.7882, blue: 0.7961, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                info: Status(base: Color(.sRGB, red: 0.1176, green: 0.3725, blue: 0.7490, opacity: 1.0000), content: Color(.sRGB, red: 0.1176, green: 0.3725, blue: 0.7490, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9059, green: 0.9412, blue: 0.9922, opacity: 1.0000), border: Color(.sRGB, red: 0.7647, green: 0.8471, blue: 0.9686, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                neutral: Status(base: Color(.sRGB, red: 0.3529, green: 0.3922, blue: 0.4471, opacity: 1.0000), content: Color(.sRGB, red: 0.3529, green: 0.3922, blue: 0.4471, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9373, green: 0.9451, blue: 0.9608, opacity: 1.0000), border: Color(.sRGB, red: 0.8902, green: 0.9020, blue: 0.9255, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000))
            )
            static let dark = Roles(
                background: Color(.sRGB, red: 0.0588, green: 0.0745, blue: 0.1137, opacity: 1.0000),
                surface: Color(.sRGB, red: 0.0902, green: 0.1176, blue: 0.1686, opacity: 1.0000),
                surfaceVariant: Color(.sRGB, red: 0.1294, green: 0.1647, blue: 0.2275, opacity: 1.0000),
                surfaceInverse: Color(.sRGB, red: 0.9490, green: 0.9569, blue: 0.9725, opacity: 1.0000),
                contentPrimary: Color(.sRGB, red: 0.9490, green: 0.9569, blue: 0.9725, opacity: 1.0000),
                contentSecondary: Color(.sRGB, red: 0.7059, green: 0.7373, blue: 0.7922, opacity: 1.0000),
                contentTertiary: Color(.sRGB, red: 0.4980, green: 0.5333, blue: 0.6000, opacity: 1.0000),
                contentDisabled: Color(.sRGB, red: 0.3373, green: 0.3725, blue: 0.4314, opacity: 1.0000),
                contentInverse: Color(.sRGB, red: 0.0431, green: 0.0549, blue: 0.0784, opacity: 1.0000),
                border: Color(.sRGB, red: 0.1686, green: 0.2118, blue: 0.2902, opacity: 1.0000),
                borderStrong: Color(.sRGB, red: 0.2196, green: 0.2549, blue: 0.3098, opacity: 1.0000),
                divider: Color(.sRGB, red: 0.1373, green: 0.1765, blue: 0.2431, opacity: 1.0000),
                backgroundGrouped: Color(.sRGB, red: 0.0588, green: 0.0745, blue: 0.1137, opacity: 1.0000),
                surfaceGrouped: Color(.sRGB, red: 0.0784, green: 0.0980, blue: 0.1333, opacity: 1.0000),
                surfaceElevated: Color(.sRGB, red: 0.1137, green: 0.1373, blue: 0.1804, opacity: 1.0000),
                fill: Color(.sRGB, red: 0.1137, green: 0.1373, blue: 0.1804, opacity: 1.0000),
                separator: Color(.sRGB, red: 0.1490, green: 0.1765, blue: 0.2275, opacity: 1.0000),
                highlight: Color(.sRGB, red: 0.1490, green: 0.1765, blue: 0.2275, opacity: 1.0000),
                skeleton: Color(.sRGB, red: 0.1333, green: 0.1686, blue: 0.2314, opacity: 1.0000),
                scrim: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.6000),
                shadow: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.4000),
                focusRing: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 0.4510),
                accent: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 1.0000),
                accentPressed: Color(.sRGB, red: 0.4824, green: 0.6471, blue: 0.9608, opacity: 1.0000),
                accentSubtle: Color(.sRGB, red: 0.0863, green: 0.1373, blue: 0.2314, opacity: 1.0000),
                accentContent: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 1.0000),
                onAccent: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                secondary: Color(.sRGB, red: 0.5294, green: 0.6078, blue: 0.7725, opacity: 1.0000),
                secondaryPressed: Color(.sRGB, red: 0.6431, green: 0.7020, blue: 0.8157, opacity: 1.0000),
                secondarySubtle: Color(.sRGB, red: 0.0902, green: 0.1216, blue: 0.1922, opacity: 1.0000),
                secondaryContent: Color(.sRGB, red: 0.5294, green: 0.6078, blue: 0.7725, opacity: 1.0000),
                onSecondary: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                tertiary: Color(.sRGB, red: 0.7451, green: 0.3765, blue: 0.9216, opacity: 1.0000),
                tertiaryPressed: Color(.sRGB, red: 0.7961, green: 0.5373, blue: 0.9255, opacity: 1.0000),
                tertiarySubtle: Color(.sRGB, red: 0.1333, green: 0.0784, blue: 0.2196, opacity: 1.0000),
                tertiaryContent: Color(.sRGB, red: 0.7451, green: 0.3765, blue: 0.9216, opacity: 1.0000),
                onTertiary: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                success: Status(base: Color(.sRGB, red: 0.2392, green: 0.8314, blue: 0.6275, opacity: 1.0000), content: Color(.sRGB, red: 0.2392, green: 0.8314, blue: 0.6275, opacity: 1.0000), subtle: Color(.sRGB, red: 0.0627, green: 0.1529, blue: 0.1216, opacity: 1.0000), border: Color(.sRGB, red: 0.1176, green: 0.2745, blue: 0.2196, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                warning: Status(base: Color(.sRGB, red: 0.9137, green: 0.6353, blue: 0.2314, opacity: 1.0000), content: Color(.sRGB, red: 0.9137, green: 0.6353, blue: 0.2314, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1647, green: 0.1216, blue: 0.0549, opacity: 1.0000), border: Color(.sRGB, red: 0.2902, green: 0.2118, blue: 0.0863, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                danger: Status(base: Color(.sRGB, red: 0.9412, green: 0.4000, blue: 0.4275, opacity: 1.0000), content: Color(.sRGB, red: 0.9412, green: 0.4000, blue: 0.4275, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1686, green: 0.0784, blue: 0.0863, opacity: 1.0000), border: Color(.sRGB, red: 0.3059, green: 0.1294, blue: 0.1490, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                info: Status(base: Color(.sRGB, red: 0.4353, green: 0.6510, blue: 1.0000, opacity: 1.0000), content: Color(.sRGB, red: 0.4353, green: 0.6510, blue: 1.0000, opacity: 1.0000), subtle: Color(.sRGB, red: 0.0627, green: 0.1255, blue: 0.2275, opacity: 1.0000), border: Color(.sRGB, red: 0.1176, green: 0.2196, blue: 0.3843, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                neutral: Status(base: Color(.sRGB, red: 0.7059, green: 0.7373, blue: 0.7922, opacity: 1.0000), content: Color(.sRGB, red: 0.7059, green: 0.7373, blue: 0.7922, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1020, green: 0.1255, blue: 0.1686, opacity: 1.0000), border: Color(.sRGB, red: 0.1725, green: 0.2039, blue: 0.2549, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000))
            )
        }

        nonisolated enum Paper {
            static let light = Roles(
                background: Color(.sRGB, red: 0.9686, green: 0.9569, blue: 0.9333, opacity: 1.0000),
                surface: Color(.sRGB, red: 1.0000, green: 0.9922, blue: 0.9765, opacity: 1.0000),
                surfaceVariant: Color(.sRGB, red: 0.9373, green: 0.9176, blue: 0.8824, opacity: 1.0000),
                surfaceInverse: Color(.sRGB, red: 0.0431, green: 0.0588, blue: 0.1020, opacity: 1.0000),
                contentPrimary: Color(.sRGB, red: 0.0824, green: 0.0745, blue: 0.0588, opacity: 1.0000),
                contentSecondary: Color(.sRGB, red: 0.2471, green: 0.2235, blue: 0.1922, opacity: 1.0000),
                contentTertiary: Color(.sRGB, red: 0.4157, green: 0.3843, blue: 0.3373, opacity: 1.0000),
                contentDisabled: Color(.sRGB, red: 0.6392, green: 0.6667, blue: 0.7137, opacity: 1.0000),
                contentInverse: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                border: Color(.sRGB, red: 0.8863, green: 0.8588, blue: 0.8118, opacity: 1.0000),
                borderStrong: Color(.sRGB, red: 0.8000, green: 0.7608, blue: 0.6941, opacity: 1.0000),
                divider: Color(.sRGB, red: 0.9176, green: 0.8941, blue: 0.8549, opacity: 1.0000),
                backgroundGrouped: Color(.sRGB, red: 0.9373, green: 0.9176, blue: 0.8824, opacity: 1.0000),
                surfaceGrouped: Color(.sRGB, red: 1.0000, green: 0.9922, blue: 0.9765, opacity: 1.0000),
                surfaceElevated: Color(.sRGB, red: 1.0000, green: 0.9922, blue: 0.9765, opacity: 1.0000),
                fill: Color(.sRGB, red: 0.9176, green: 0.8941, blue: 0.8549, opacity: 1.0000),
                separator: Color(.sRGB, red: 0.8863, green: 0.8588, blue: 0.8118, opacity: 1.0000),
                highlight: Color(.sRGB, red: 0.9176, green: 0.8941, blue: 0.8549, opacity: 1.0000),
                skeleton: Color(.sRGB, red: 0.9176, green: 0.8941, blue: 0.8549, opacity: 1.0000),
                scrim: Color(.sRGB, red: 0.0431, green: 0.0588, blue: 0.1020, opacity: 0.4510),
                shadow: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.1216),
                focusRing: Color(.sRGB, red: 0.1725, green: 0.4196, blue: 0.9294, opacity: 0.4000),
                accent: Color(.sRGB, red: 0.1725, green: 0.4196, blue: 0.9294, opacity: 1.0000),
                accentPressed: Color(.sRGB, red: 0.1216, green: 0.3216, blue: 0.7686, opacity: 1.0000),
                accentSubtle: Color(.sRGB, red: 0.9098, green: 0.9373, blue: 0.9922, opacity: 1.0000),
                accentContent: Color(.sRGB, red: 0.1098, green: 0.3765, blue: 0.9255, opacity: 1.0000),
                onAccent: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                secondary: Color(.sRGB, red: 0.3804, green: 0.4902, blue: 0.7216, opacity: 1.0000),
                secondaryPressed: Color(.sRGB, red: 0.2667, green: 0.3725, blue: 0.5922, opacity: 1.0000),
                secondarySubtle: Color(.sRGB, red: 0.9373, green: 0.9490, blue: 0.9725, opacity: 1.0000),
                secondaryContent: Color(.sRGB, red: 0.3020, green: 0.4196, blue: 0.6706, opacity: 1.0000),
                onSecondary: Color(.sRGB, red: 0.0431, green: 0.0549, blue: 0.0784, opacity: 1.0000),
                tertiary: Color(.sRGB, red: 0.6824, green: 0.1725, blue: 0.9294, opacity: 1.0000),
                tertiaryPressed: Color(.sRGB, red: 0.5569, green: 0.0667, blue: 0.7922, opacity: 1.0000),
                tertiarySubtle: Color(.sRGB, red: 0.9686, green: 0.9176, blue: 0.9922, opacity: 1.0000),
                tertiaryContent: Color(.sRGB, red: 0.6588, green: 0.1098, blue: 0.9255, opacity: 1.0000),
                onTertiary: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                success: Status(base: Color(.sRGB, red: 0.0667, green: 0.4863, blue: 0.3490, opacity: 1.0000), content: Color(.sRGB, red: 0.0667, green: 0.4863, blue: 0.3490, opacity: 1.0000), subtle: Color(.sRGB, red: 0.8941, green: 0.9608, blue: 0.9333, opacity: 1.0000), border: Color(.sRGB, red: 0.7176, green: 0.8863, blue: 0.8235, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                warning: Status(base: Color(.sRGB, red: 0.6392, green: 0.3569, blue: 0.0000, opacity: 1.0000), content: Color(.sRGB, red: 0.6392, green: 0.3569, blue: 0.0000, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9922, green: 0.9451, blue: 0.8784, opacity: 1.0000), border: Color(.sRGB, red: 0.9490, green: 0.8667, blue: 0.7216, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                danger: Status(base: Color(.sRGB, red: 0.7686, green: 0.1686, blue: 0.1961, opacity: 1.0000), content: Color(.sRGB, red: 0.7686, green: 0.1686, blue: 0.1961, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9882, green: 0.9137, blue: 0.9137, opacity: 1.0000), border: Color(.sRGB, red: 0.9529, green: 0.7882, blue: 0.7961, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                info: Status(base: Color(.sRGB, red: 0.1176, green: 0.3725, blue: 0.7490, opacity: 1.0000), content: Color(.sRGB, red: 0.1176, green: 0.3725, blue: 0.7490, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9059, green: 0.9412, blue: 0.9922, opacity: 1.0000), border: Color(.sRGB, red: 0.7647, green: 0.8471, blue: 0.9686, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                neutral: Status(base: Color(.sRGB, red: 0.3529, green: 0.3922, blue: 0.4471, opacity: 1.0000), content: Color(.sRGB, red: 0.3529, green: 0.3922, blue: 0.4471, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9373, green: 0.9451, blue: 0.9608, opacity: 1.0000), border: Color(.sRGB, red: 0.8902, green: 0.9020, blue: 0.9255, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000))
            )
            static let dark = Roles(
                background: Color(.sRGB, red: 0.0706, green: 0.0667, blue: 0.0588, opacity: 1.0000),
                surface: Color(.sRGB, red: 0.1059, green: 0.0980, blue: 0.0863, opacity: 1.0000),
                surfaceVariant: Color(.sRGB, red: 0.1451, green: 0.1333, blue: 0.1176, opacity: 1.0000),
                surfaceInverse: Color(.sRGB, red: 0.9490, green: 0.9569, blue: 0.9725, opacity: 1.0000),
                contentPrimary: Color(.sRGB, red: 0.9569, green: 0.9373, blue: 0.9020, opacity: 1.0000),
                contentSecondary: Color(.sRGB, red: 0.7961, green: 0.7608, blue: 0.7059, opacity: 1.0000),
                contentTertiary: Color(.sRGB, red: 0.6000, green: 0.5647, blue: 0.5020, opacity: 1.0000),
                contentDisabled: Color(.sRGB, red: 0.3373, green: 0.3725, blue: 0.4314, opacity: 1.0000),
                contentInverse: Color(.sRGB, red: 0.0431, green: 0.0549, blue: 0.0784, opacity: 1.0000),
                border: Color(.sRGB, red: 0.2039, green: 0.1882, blue: 0.1647, opacity: 1.0000),
                borderStrong: Color(.sRGB, red: 0.2902, green: 0.2667, blue: 0.2314, opacity: 1.0000),
                divider: Color(.sRGB, red: 0.1529, green: 0.1412, blue: 0.1216, opacity: 1.0000),
                backgroundGrouped: Color(.sRGB, red: 0.0706, green: 0.0667, blue: 0.0588, opacity: 1.0000),
                surfaceGrouped: Color(.sRGB, red: 0.1059, green: 0.0980, blue: 0.0863, opacity: 1.0000),
                surfaceElevated: Color(.sRGB, red: 0.1451, green: 0.1333, blue: 0.1176, opacity: 1.0000),
                fill: Color(.sRGB, red: 0.1451, green: 0.1333, blue: 0.1176, opacity: 1.0000),
                separator: Color(.sRGB, red: 0.2039, green: 0.1882, blue: 0.1647, opacity: 1.0000),
                highlight: Color(.sRGB, red: 0.2039, green: 0.1882, blue: 0.1647, opacity: 1.0000),
                skeleton: Color(.sRGB, red: 0.1529, green: 0.1412, blue: 0.1216, opacity: 1.0000),
                scrim: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.6000),
                shadow: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.4000),
                focusRing: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 0.4510),
                accent: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 1.0000),
                accentPressed: Color(.sRGB, red: 0.4824, green: 0.6471, blue: 0.9608, opacity: 1.0000),
                accentSubtle: Color(.sRGB, red: 0.0863, green: 0.1373, blue: 0.2314, opacity: 1.0000),
                accentContent: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 1.0000),
                onAccent: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                secondary: Color(.sRGB, red: 0.5294, green: 0.6078, blue: 0.7725, opacity: 1.0000),
                secondaryPressed: Color(.sRGB, red: 0.6431, green: 0.7020, blue: 0.8157, opacity: 1.0000),
                secondarySubtle: Color(.sRGB, red: 0.0902, green: 0.1216, blue: 0.1922, opacity: 1.0000),
                secondaryContent: Color(.sRGB, red: 0.5294, green: 0.6078, blue: 0.7725, opacity: 1.0000),
                onSecondary: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                tertiary: Color(.sRGB, red: 0.7451, green: 0.3765, blue: 0.9216, opacity: 1.0000),
                tertiaryPressed: Color(.sRGB, red: 0.7961, green: 0.5373, blue: 0.9255, opacity: 1.0000),
                tertiarySubtle: Color(.sRGB, red: 0.1333, green: 0.0784, blue: 0.2196, opacity: 1.0000),
                tertiaryContent: Color(.sRGB, red: 0.7451, green: 0.3765, blue: 0.9216, opacity: 1.0000),
                onTertiary: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                success: Status(base: Color(.sRGB, red: 0.2392, green: 0.8314, blue: 0.6275, opacity: 1.0000), content: Color(.sRGB, red: 0.2392, green: 0.8314, blue: 0.6275, opacity: 1.0000), subtle: Color(.sRGB, red: 0.0627, green: 0.1529, blue: 0.1216, opacity: 1.0000), border: Color(.sRGB, red: 0.1176, green: 0.2745, blue: 0.2196, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                warning: Status(base: Color(.sRGB, red: 0.9137, green: 0.6353, blue: 0.2314, opacity: 1.0000), content: Color(.sRGB, red: 0.9137, green: 0.6353, blue: 0.2314, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1647, green: 0.1216, blue: 0.0549, opacity: 1.0000), border: Color(.sRGB, red: 0.2902, green: 0.2118, blue: 0.0863, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                danger: Status(base: Color(.sRGB, red: 0.9412, green: 0.4000, blue: 0.4275, opacity: 1.0000), content: Color(.sRGB, red: 0.9412, green: 0.4000, blue: 0.4275, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1686, green: 0.0784, blue: 0.0863, opacity: 1.0000), border: Color(.sRGB, red: 0.3059, green: 0.1294, blue: 0.1490, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                info: Status(base: Color(.sRGB, red: 0.4353, green: 0.6510, blue: 1.0000, opacity: 1.0000), content: Color(.sRGB, red: 0.4353, green: 0.6510, blue: 1.0000, opacity: 1.0000), subtle: Color(.sRGB, red: 0.0627, green: 0.1255, blue: 0.2275, opacity: 1.0000), border: Color(.sRGB, red: 0.1176, green: 0.2196, blue: 0.3843, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                neutral: Status(base: Color(.sRGB, red: 0.7059, green: 0.7373, blue: 0.7922, opacity: 1.0000), content: Color(.sRGB, red: 0.7059, green: 0.7373, blue: 0.7922, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1020, green: 0.1255, blue: 0.1686, opacity: 1.0000), border: Color(.sRGB, red: 0.1725, green: 0.2039, blue: 0.2549, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000))
            )
        }

        nonisolated enum Cream {
            static let light = Roles(
                background: Color(.sRGB, red: 1.0000, green: 0.9686, blue: 0.9176, opacity: 1.0000),
                surface: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                surfaceVariant: Color(.sRGB, red: 1.0000, green: 0.9373, blue: 0.8392, opacity: 1.0000),
                surfaceInverse: Color(.sRGB, red: 0.0431, green: 0.0588, blue: 0.1020, opacity: 1.0000),
                contentPrimary: Color(.sRGB, red: 0.1020, green: 0.0706, blue: 0.0235, opacity: 1.0000),
                contentSecondary: Color(.sRGB, red: 0.2706, green: 0.2235, blue: 0.1647, opacity: 1.0000),
                contentTertiary: Color(.sRGB, red: 0.4353, green: 0.3765, blue: 0.2824, opacity: 1.0000),
                contentDisabled: Color(.sRGB, red: 0.6392, green: 0.6667, blue: 0.7137, opacity: 1.0000),
                contentInverse: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                border: Color(.sRGB, red: 0.9412, green: 0.8745, blue: 0.7608, opacity: 1.0000),
                borderStrong: Color(.sRGB, red: 0.8667, green: 0.7725, blue: 0.6157, opacity: 1.0000),
                divider: Color(.sRGB, red: 0.9647, green: 0.9098, blue: 0.8157, opacity: 1.0000),
                backgroundGrouped: Color(.sRGB, red: 1.0000, green: 0.9373, blue: 0.8392, opacity: 1.0000),
                surfaceGrouped: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                surfaceElevated: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                fill: Color(.sRGB, red: 0.9647, green: 0.9098, blue: 0.8157, opacity: 1.0000),
                separator: Color(.sRGB, red: 0.9412, green: 0.8745, blue: 0.7608, opacity: 1.0000),
                highlight: Color(.sRGB, red: 0.9647, green: 0.9098, blue: 0.8157, opacity: 1.0000),
                skeleton: Color(.sRGB, red: 0.9647, green: 0.9098, blue: 0.8157, opacity: 1.0000),
                scrim: Color(.sRGB, red: 0.0431, green: 0.0588, blue: 0.1020, opacity: 0.4510),
                shadow: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.1216),
                focusRing: Color(.sRGB, red: 0.1725, green: 0.4196, blue: 0.9294, opacity: 0.4000),
                accent: Color(.sRGB, red: 0.1725, green: 0.4196, blue: 0.9294, opacity: 1.0000),
                accentPressed: Color(.sRGB, red: 0.1216, green: 0.3216, blue: 0.7686, opacity: 1.0000),
                accentSubtle: Color(.sRGB, red: 0.9098, green: 0.9373, blue: 0.9922, opacity: 1.0000),
                accentContent: Color(.sRGB, red: 0.1098, green: 0.3765, blue: 0.9255, opacity: 1.0000),
                onAccent: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                secondary: Color(.sRGB, red: 0.3804, green: 0.4902, blue: 0.7216, opacity: 1.0000),
                secondaryPressed: Color(.sRGB, red: 0.2667, green: 0.3725, blue: 0.5922, opacity: 1.0000),
                secondarySubtle: Color(.sRGB, red: 0.9373, green: 0.9490, blue: 0.9725, opacity: 1.0000),
                secondaryContent: Color(.sRGB, red: 0.3020, green: 0.4196, blue: 0.6706, opacity: 1.0000),
                onSecondary: Color(.sRGB, red: 0.0431, green: 0.0549, blue: 0.0784, opacity: 1.0000),
                tertiary: Color(.sRGB, red: 0.6824, green: 0.1725, blue: 0.9294, opacity: 1.0000),
                tertiaryPressed: Color(.sRGB, red: 0.5569, green: 0.0667, blue: 0.7922, opacity: 1.0000),
                tertiarySubtle: Color(.sRGB, red: 0.9686, green: 0.9176, blue: 0.9922, opacity: 1.0000),
                tertiaryContent: Color(.sRGB, red: 0.6824, green: 0.1725, blue: 0.9294, opacity: 1.0000),
                onTertiary: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000),
                success: Status(base: Color(.sRGB, red: 0.0667, green: 0.4863, blue: 0.3490, opacity: 1.0000), content: Color(.sRGB, red: 0.0667, green: 0.4863, blue: 0.3490, opacity: 1.0000), subtle: Color(.sRGB, red: 0.8941, green: 0.9608, blue: 0.9333, opacity: 1.0000), border: Color(.sRGB, red: 0.7176, green: 0.8863, blue: 0.8235, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                warning: Status(base: Color(.sRGB, red: 0.6392, green: 0.3569, blue: 0.0000, opacity: 1.0000), content: Color(.sRGB, red: 0.6392, green: 0.3569, blue: 0.0000, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9922, green: 0.9451, blue: 0.8784, opacity: 1.0000), border: Color(.sRGB, red: 0.9490, green: 0.8667, blue: 0.7216, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                danger: Status(base: Color(.sRGB, red: 0.7686, green: 0.1686, blue: 0.1961, opacity: 1.0000), content: Color(.sRGB, red: 0.7686, green: 0.1686, blue: 0.1961, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9882, green: 0.9137, blue: 0.9137, opacity: 1.0000), border: Color(.sRGB, red: 0.9529, green: 0.7882, blue: 0.7961, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                info: Status(base: Color(.sRGB, red: 0.1176, green: 0.3725, blue: 0.7490, opacity: 1.0000), content: Color(.sRGB, red: 0.1176, green: 0.3725, blue: 0.7490, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9059, green: 0.9412, blue: 0.9922, opacity: 1.0000), border: Color(.sRGB, red: 0.7647, green: 0.8471, blue: 0.9686, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000)),
                neutral: Status(base: Color(.sRGB, red: 0.3529, green: 0.3922, blue: 0.4471, opacity: 1.0000), content: Color(.sRGB, red: 0.3529, green: 0.3922, blue: 0.4471, opacity: 1.0000), subtle: Color(.sRGB, red: 0.9373, green: 0.9451, blue: 0.9608, opacity: 1.0000), border: Color(.sRGB, red: 0.8902, green: 0.9020, blue: 0.9255, opacity: 1.0000), on: Color(.sRGB, red: 1.0000, green: 1.0000, blue: 1.0000, opacity: 1.0000))
            )
            static let dark = Roles(
                background: Color(.sRGB, red: 0.0824, green: 0.0706, blue: 0.0510, opacity: 1.0000),
                surface: Color(.sRGB, red: 0.1216, green: 0.1020, blue: 0.0745, opacity: 1.0000),
                surfaceVariant: Color(.sRGB, red: 0.1647, green: 0.1412, blue: 0.0980, opacity: 1.0000),
                surfaceInverse: Color(.sRGB, red: 0.9490, green: 0.9569, blue: 0.9725, opacity: 1.0000),
                contentPrimary: Color(.sRGB, red: 0.9843, green: 0.9529, blue: 0.8941, opacity: 1.0000),
                contentSecondary: Color(.sRGB, red: 0.8392, green: 0.7843, blue: 0.6902, opacity: 1.0000),
                contentTertiary: Color(.sRGB, red: 0.6392, green: 0.5804, blue: 0.4824, opacity: 1.0000),
                contentDisabled: Color(.sRGB, red: 0.3373, green: 0.3725, blue: 0.4314, opacity: 1.0000),
                contentInverse: Color(.sRGB, red: 0.0431, green: 0.0549, blue: 0.0784, opacity: 1.0000),
                border: Color(.sRGB, red: 0.2275, green: 0.1961, blue: 0.1412, opacity: 1.0000),
                borderStrong: Color(.sRGB, red: 0.3176, green: 0.2745, blue: 0.2039, opacity: 1.0000),
                divider: Color(.sRGB, red: 0.1686, green: 0.1451, blue: 0.0980, opacity: 1.0000),
                backgroundGrouped: Color(.sRGB, red: 0.0824, green: 0.0706, blue: 0.0510, opacity: 1.0000),
                surfaceGrouped: Color(.sRGB, red: 0.1216, green: 0.1020, blue: 0.0745, opacity: 1.0000),
                surfaceElevated: Color(.sRGB, red: 0.1647, green: 0.1412, blue: 0.0980, opacity: 1.0000),
                fill: Color(.sRGB, red: 0.1647, green: 0.1412, blue: 0.0980, opacity: 1.0000),
                separator: Color(.sRGB, red: 0.2275, green: 0.1961, blue: 0.1412, opacity: 1.0000),
                highlight: Color(.sRGB, red: 0.2275, green: 0.1961, blue: 0.1412, opacity: 1.0000),
                skeleton: Color(.sRGB, red: 0.1686, green: 0.1451, blue: 0.0980, opacity: 1.0000),
                scrim: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.6000),
                shadow: Color(.sRGB, red: 0.0000, green: 0.0000, blue: 0.0000, opacity: 0.4000),
                focusRing: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 0.4510),
                accent: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 1.0000),
                accentPressed: Color(.sRGB, red: 0.4824, green: 0.6471, blue: 0.9608, opacity: 1.0000),
                accentSubtle: Color(.sRGB, red: 0.0863, green: 0.1373, blue: 0.2314, opacity: 1.0000),
                accentContent: Color(.sRGB, red: 0.3569, green: 0.5529, blue: 0.9373, opacity: 1.0000),
                onAccent: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                secondary: Color(.sRGB, red: 0.5294, green: 0.6078, blue: 0.7725, opacity: 1.0000),
                secondaryPressed: Color(.sRGB, red: 0.6431, green: 0.7020, blue: 0.8157, opacity: 1.0000),
                secondarySubtle: Color(.sRGB, red: 0.0902, green: 0.1216, blue: 0.1922, opacity: 1.0000),
                secondaryContent: Color(.sRGB, red: 0.5294, green: 0.6078, blue: 0.7725, opacity: 1.0000),
                onSecondary: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                tertiary: Color(.sRGB, red: 0.7451, green: 0.3765, blue: 0.9216, opacity: 1.0000),
                tertiaryPressed: Color(.sRGB, red: 0.7961, green: 0.5373, blue: 0.9255, opacity: 1.0000),
                tertiarySubtle: Color(.sRGB, red: 0.1333, green: 0.0784, blue: 0.2196, opacity: 1.0000),
                tertiaryContent: Color(.sRGB, red: 0.7451, green: 0.3765, blue: 0.9216, opacity: 1.0000),
                onTertiary: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000),
                success: Status(base: Color(.sRGB, red: 0.2392, green: 0.8314, blue: 0.6275, opacity: 1.0000), content: Color(.sRGB, red: 0.2392, green: 0.8314, blue: 0.6275, opacity: 1.0000), subtle: Color(.sRGB, red: 0.0627, green: 0.1529, blue: 0.1216, opacity: 1.0000), border: Color(.sRGB, red: 0.1176, green: 0.2745, blue: 0.2196, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                warning: Status(base: Color(.sRGB, red: 0.9137, green: 0.6353, blue: 0.2314, opacity: 1.0000), content: Color(.sRGB, red: 0.9137, green: 0.6353, blue: 0.2314, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1647, green: 0.1216, blue: 0.0549, opacity: 1.0000), border: Color(.sRGB, red: 0.2902, green: 0.2118, blue: 0.0863, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                danger: Status(base: Color(.sRGB, red: 0.9412, green: 0.4000, blue: 0.4275, opacity: 1.0000), content: Color(.sRGB, red: 0.9412, green: 0.4000, blue: 0.4275, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1686, green: 0.0784, blue: 0.0863, opacity: 1.0000), border: Color(.sRGB, red: 0.3059, green: 0.1294, blue: 0.1490, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                info: Status(base: Color(.sRGB, red: 0.4353, green: 0.6510, blue: 1.0000, opacity: 1.0000), content: Color(.sRGB, red: 0.4353, green: 0.6510, blue: 1.0000, opacity: 1.0000), subtle: Color(.sRGB, red: 0.0627, green: 0.1255, blue: 0.2275, opacity: 1.0000), border: Color(.sRGB, red: 0.1176, green: 0.2196, blue: 0.3843, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000)),
                neutral: Status(base: Color(.sRGB, red: 0.7059, green: 0.7373, blue: 0.7922, opacity: 1.0000), content: Color(.sRGB, red: 0.7059, green: 0.7373, blue: 0.7922, opacity: 1.0000), subtle: Color(.sRGB, red: 0.1020, green: 0.1255, blue: 0.1686, opacity: 1.0000), border: Color(.sRGB, red: 0.1725, green: 0.2039, blue: 0.2549, opacity: 1.0000), on: Color(.sRGB, red: 0.0235, green: 0.0627, blue: 0.1216, opacity: 1.0000))
            )
        }
    }

    nonisolated struct Style {
        let radiusXs: CGFloat
        let radiusSm: CGFloat
        let radiusMd: CGFloat
        let radiusLg: CGFloat
        let radiusXl: CGFloat
        let radiusSheet: CGFloat
        /// Nil: a capsule, as round as the control is tall.
        let buttonRadius: CGFloat?
        let chipRadius: CGFloat?
        let card: CardTreatment
        let field: FieldTreatment
        let borderWidth: CGFloat
        let borderWidthStrong: CGFloat
        let uppercaseLabels: Bool
        let offsetShadow: CGFloat
        let light: Roles
        let dark: Roles
        let voice: TypeVoice
    }

    nonisolated enum Styles {
        static let utility = Style(
            radiusXs: 6,
            radiusSm: 10,
            radiusMd: 14,
            radiusLg: 20,
            radiusXl: 28,
            radiusSheet: 24,
            buttonRadius: 10,
            chipRadius: nil,
            card: .outlined,
            field: .outlined,
            borderWidth: 1,
            borderWidthStrong: 1.5,
            uppercaseLabels: false,
            offsetShadow: 0,
            light: Palette.Cool.light,
            dark: Palette.Cool.dark,
            voice: .neutral
        )
        static let social = Style(
            radiusXs: 10,
            radiusSm: 14,
            radiusMd: 20,
            radiusLg: 26,
            radiusXl: 32,
            radiusSheet: 30,
            buttonRadius: nil,
            chipRadius: nil,
            card: .raised,
            field: .filled,
            borderWidth: 1,
            borderWidthStrong: 1.5,
            uppercaseLabels: false,
            offsetShadow: 0,
            light: Palette.BrandTint.light,
            dark: Palette.BrandTint.dark,
            voice: .bold
        )
        static let editorial = Style(
            radiusXs: 2,
            radiusSm: 3,
            radiusMd: 4,
            radiusLg: 6,
            radiusXl: 8,
            radiusSheet: 8,
            buttonRadius: 2,
            chipRadius: 2,
            card: .outlined,
            field: .underlined,
            borderWidth: 1,
            borderWidthStrong: 1.5,
            uppercaseLabels: true,
            offsetShadow: 0,
            light: Palette.Paper.light,
            dark: Palette.Paper.dark,
            voice: .editorial
        )
        static let playful = Style(
            radiusXs: 8,
            radiusSm: 12,
            radiusMd: 16,
            radiusLg: 22,
            radiusXl: 28,
            radiusSheet: 28,
            buttonRadius: 14,
            chipRadius: 12,
            card: .offset,
            field: .outlined,
            borderWidth: 2,
            borderWidthStrong: 2.5,
            uppercaseLabels: false,
            offsetShadow: 4,
            light: Palette.Cream.light,
            dark: Palette.Cream.dark,
            voice: .rounded
        )

        static func style(_ style: AppDesignStyle) -> Style {
            switch style {
            case .utility: utility
            case .social: social
            case .editorial: editorial
            case .playful: playful
            }
        }
    }

    /// The choices the project was generated with.
    nonisolated enum Defaults {
        static let designStyle = AppDesignStyle.utility
        static let motionStyle = AppMotionStyle.standard
        static let hapticsEnabled = true
    }
}
