import SwiftUI

extension View {
    /// Provides the design system to this view and everything inside it, resolved for the current
    /// appearance, and tints system controls with the accent.
    public func appTheme(
        designStyle: AppDesignStyle = AppTheme.defaultDesignStyle,
        motionStyle: AppMotionStyle = AppTheme.defaultMotionStyle,
        hapticsEnabled: Bool = AppTheme.defaultHapticsEnabled
    ) -> some View {
        modifier(AppThemeProvider(designStyle: designStyle, motionStyle: motionStyle, hapticsEnabled: hapticsEnabled))
    }
}

private struct AppThemeProvider: ViewModifier {
    let designStyle: AppDesignStyle
    let motionStyle: AppMotionStyle
    let hapticsEnabled: Bool

    @Environment(\.colorScheme) private var colorScheme

    func body(content: Content) -> some View {
        let theme = AppTheme(
            designStyle: designStyle,
            motionStyle: motionStyle,
            colorScheme: colorScheme,
            hapticsEnabled: hapticsEnabled
        )
        content
            .environment(\.appTheme, theme)
            .tint(theme.colors.accent)
    }
}
