import SwiftUI

/// Press feedback for a tappable row outside a `List`: the row's background takes the highlight
/// colour at once and lets it go over a fifth of a second.
///
/// Rows inside a `List` already highlight the way the system does; this is for rows in a
/// `ScrollView` or a `LazyVStack`.
public struct HighlightRowStyle: ButtonStyle {
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        HighlightRowBody(label: configuration.label, isPressed: configuration.isPressed)
    }
}

extension ButtonStyle where Self == HighlightRowStyle {
    /// Highlights the whole row under the finger. For tappable rows outside a `List`.
    public static var highlightRow: HighlightRowStyle {
        HighlightRowStyle()
    }
}

private struct HighlightRowBody: View {
    let label: ButtonStyleConfiguration.Label
    let isPressed: Bool

    @Environment(\.appTheme) private var theme

    var body: some View {
        label
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(.rect)
            .background(isPressed ? theme.colors.highlight : .clear)
            .animation(
                isPressed ? nil : .easeOut(duration: DesignTokens.Motion.highlightFadeOut),
                value: isPressed
            )
            .hoverEffect(.highlight)
    }
}
