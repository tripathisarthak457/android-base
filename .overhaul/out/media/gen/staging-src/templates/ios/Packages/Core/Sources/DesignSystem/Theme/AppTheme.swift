import SwiftUI

/// The design system resolved for one design style, motion style and appearance.
///
/// Read it with `@Environment(\.appTheme)`. `View.appTheme(designStyle:motionStyle:hapticsEnabled:)`
/// at the root provides it and resolves it again when the appearance changes, so views never
/// branch on `colorScheme` themselves. Spacing and sizes do not depend on any of this and are
/// static: `AppSpacing.lg`, `AppSizes.minTouchTarget`.
public nonisolated struct AppTheme: Equatable, Sendable {
    public let designStyle: AppDesignStyle
    public let motionStyle: AppMotionStyle
    public let colorScheme: ColorScheme
    public let colors: AppColors
    public let typography: AppTypography
    public let shapes: AppShapes
    public let springs: AppSprings
    /// Whether the app plays haptics. The person's system setting applies on top.
    public let hapticsEnabled: Bool

    public init(
        designStyle: AppDesignStyle,
        motionStyle: AppMotionStyle,
        colorScheme: ColorScheme,
        hapticsEnabled: Bool
    ) {
        let style = DesignTokens.Styles.style(designStyle)
        self.designStyle = designStyle
        self.motionStyle = motionStyle
        self.colorScheme = colorScheme
        colors = AppColors(roles: colorScheme == .dark ? style.dark : style.light)
        typography = AppTypography(voice: style.voice)
        shapes = AppShapes(style: style)
        springs = AppSprings(motion: DesignTokens.Motion.style(motionStyle))
        self.hapticsEnabled = hapticsEnabled
    }

    // Everything else is derived from these four, so comparing them is enough.
    public static func == (lhs: AppTheme, rhs: AppTheme) -> Bool {
        lhs.designStyle == rhs.designStyle && lhs.motionStyle == rhs.motionStyle
            && lhs.colorScheme == rhs.colorScheme && lhs.hapticsEnabled == rhs.hapticsEnabled
    }

    /// The design style the project was generated with.
    public static let defaultDesignStyle = DesignTokens.Defaults.designStyle

    /// The motion style the project was generated with.
    public static let defaultMotionStyle = DesignTokens.Defaults.motionStyle

    /// Whether the project was generated with haptics on.
    public static let defaultHapticsEnabled = DesignTokens.Defaults.hapticsEnabled
}

extension EnvironmentValues {
    /// The resolved design system. Outside `View.appTheme(...)` it is the defaults in light appearance.
    public nonisolated var appTheme: AppTheme {
        get { self[AppThemeKey.self] }
        set { self[AppThemeKey.self] = newValue }
    }
}

private nonisolated struct AppThemeKey: EnvironmentKey {
    static let defaultValue = AppTheme(
        designStyle: AppTheme.defaultDesignStyle,
        motionStyle: AppTheme.defaultMotionStyle,
        colorScheme: .light,
        hapticsEnabled: AppTheme.defaultHapticsEnabled
    )
}
