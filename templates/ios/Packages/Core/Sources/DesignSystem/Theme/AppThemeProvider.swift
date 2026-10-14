import SwiftUI

extension View {
    /// Provides the design system to this view and everything inside it, resolved for the current
    /// appearance, and tints system controls with the accent. The outermost one also keeps the
    /// window's shimmer clock, which every placeholder inside shares.
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
            .modifier(SkeletonClockProvider())
            .tint(theme.colors.accent)
    }
}
