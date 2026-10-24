import CoreGraphics

/// The spacing scale, in points. It is the same under every design style.
public nonisolated enum AppSpacing {
    public static let xxs = DesignTokens.Spacing.xxs
    public static let xs = DesignTokens.Spacing.xs
    public static let sm = DesignTokens.Spacing.sm
    public static let md = DesignTokens.Spacing.md
    public static let lg = DesignTokens.Spacing.lg
    public static let xl = DesignTokens.Spacing.xl
    public static let xxl = DesignTokens.Spacing.xxl
    public static let xxxl = DesignTokens.Spacing.xxxl
    /// The leading and trailing margin of a screen's content.
    public static let gutter = DesignTokens.Spacing.gutter
    /// Between sections of a screen.
    public static let section = DesignTokens.Spacing.section
    /// Between items stacked in one section.
    public static let stack = DesignTokens.Spacing.stack
}
