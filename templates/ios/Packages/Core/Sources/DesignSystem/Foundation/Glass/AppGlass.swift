import SwiftUI

extension View {
    /// Draws Liquid Glass behind this view, in `shape`.
    ///
    /// For floating chrome only: toasts, floating buttons, a custom bar. Never for content inside a
    /// scroll view, where every row would sample what is behind it on every frame.
    ///
    /// iOS 26 draws the real glass; earlier releases get the ultra-thin material, and Reduce
    /// Transparency gets a solid elevated surface. Glass views that sit together belong in one
    /// `AppGlassGroup`, so iOS renders them in one pass. Apply this after the view's other
    /// appearance modifiers, so the glass is drawn behind all of them.
    public func appGlass(in shape: some Shape) -> some View {
        modifier(AppGlassModifier(shape: shape))
    }
}

private struct AppGlassModifier<GlassShape: Shape>: ViewModifier {
    let shape: GlassShape

    @Environment(\.appTheme) private var theme
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    @ViewBuilder
    func body(content: Content) -> some View {
        if reduceTransparency {
            content.background(theme.colors.surfaceElevated, in: shape)
        } else {
            glass(content)
        }
    }

    @ViewBuilder
    private func glass(_ content: Content) -> some View {
        if #available(iOS 26, *) {
            content.glassEffect(.regular, in: shape)
        } else {
            content.background(.ultraThinMaterial, in: shape)
        }
    }
}
