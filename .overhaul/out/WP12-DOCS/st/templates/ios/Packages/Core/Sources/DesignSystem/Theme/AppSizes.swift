import CoreGraphics

/// Fixed sizes of controls, in points. They are the same under every design style.
public nonisolated enum AppSizes {
    /// The smallest tappable area, 44 points as Apple's guidelines ask.
    public static let minTouchTarget = DesignTokens.Sizes.minTouchTarget
    public static let iconSmall = DesignTokens.Sizes.iconSmall
    public static let icon = DesignTokens.Sizes.icon
    public static let iconLarge = DesignTokens.Sizes.iconLarge
    public static let buttonSmall = DesignTokens.Sizes.buttonSmall
    public static let buttonMedium = DesignTokens.Sizes.buttonMedium
    public static let buttonLarge = DesignTokens.Sizes.buttonLarge
    public static let fieldHeight = DesignTokens.Sizes.fieldHeight

    /// The symbol that leads a full-screen page, such as an onboarding step, before Dynamic Type
    /// scales it. Not a token: only the SwiftUI template draws one.
    public static let heroSymbol: CGFloat = 72
}
