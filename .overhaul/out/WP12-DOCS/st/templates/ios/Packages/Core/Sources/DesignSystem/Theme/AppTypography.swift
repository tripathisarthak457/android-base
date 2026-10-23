import SwiftUI

/// The type scale for one voice.
///
/// Every role is one of Apple's text styles, whose default sizes are the scale's sizes, so text
/// follows Dynamic Type and the Accessibility sizes without any scaling code. The voice sets the
/// weights.
public nonisolated struct AppTypography: Sendable {
    public let largeTitle: Font
    public let title1: Font
    public let title2: Font
    public let title3: Font
    public let headline: Font
    public let body: Font
    public let callout: Font
    public let subheadline: Font
    public let footnote: Font
    public let caption1: Font
    public let caption2: Font
    public let mono: Font

    init(voice: TypeVoice) {
        func font(_ role: DesignTokens.TypeRole, _ style: Font.TextStyle, design: Font.Design = .default) -> Font {
            .system(style, design: design, weight: Self.weight(of: role, in: voice))
        }
        largeTitle = font(DesignTokens.Typography.largeTitle, .largeTitle)
        title1 = font(DesignTokens.Typography.title1, .title)
        title2 = font(DesignTokens.Typography.title2, .title2)
        title3 = font(DesignTokens.Typography.title3, .title3)
        headline = font(DesignTokens.Typography.headline, .headline)
        body = font(DesignTokens.Typography.body, .body)
        callout = font(DesignTokens.Typography.callout, .callout)
        subheadline = font(DesignTokens.Typography.subheadline, .subheadline)
        footnote = font(DesignTokens.Typography.footnote, .footnote)
        caption1 = font(DesignTokens.Typography.caption1, .caption)
        caption2 = font(DesignTokens.Typography.caption2, .caption2)
        mono = font(DesignTokens.Typography.mono, .subheadline, design: .monospaced)
    }

    /// The role's weight in `voice`, as the nearest of SwiftUI's weights to the token's CSS-style
    /// number (100 thin … 900 black).
    private static func weight(of role: DesignTokens.TypeRole, in voice: TypeVoice) -> Font.Weight {
        let token = switch voice {
        case .neutral: role.weightNeutral
        case .bold: role.weightBold
        case .editorial: role.weightEditorial
        case .rounded: role.weightRounded
        }
        return switch token {
        case ..<150: .ultraLight
        case ..<250: .thin
        case ..<350: .light
        case ..<450: .regular
        case ..<550: .medium
        case ..<650: .semibold
        case ..<750: .bold
        case ..<850: .heavy
        default: .black
        }
    }
}
