import SwiftUI

/// Groups glass views that sit near each other, so iOS 26 renders their glass in one pass and lets
/// shapes closer than `spacing` merge as they move. Below iOS 26 it changes nothing.
///
/// Apple's guidance is to keep the number of glass effects on screen small; one group per cluster
/// of floating controls is the shape that keeps the cost down.
public struct AppGlassGroup<Content: View>: View {
    private let spacing: CGFloat?
    private let content: Content

    public init(spacing: CGFloat? = nil, @ViewBuilder content: () -> Content) {
        self.spacing = spacing
        self.content = content()
    }

    public var body: some View {
        if #available(iOS 26, *) {
            GlassEffectContainer(spacing: spacing) {
                content
            }
        } else {
            content
        }
    }
}
