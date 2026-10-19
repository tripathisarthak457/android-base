import SwiftUI

/// A container drawn with the design style's card treatment: a hairline outline, a soft raised
/// shadow, or a thick outline with a hard offset shadow.
public struct AppCard<Content: View>: View {
    private let content: Content

    @Environment(\.appTheme) private var theme
    @Environment(\.layoutDirection) private var layoutDirection

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        content
            .padding(AppSpacing.lg)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background {
                CardBackground(
                    treatment: theme.shapes.card,
                    shape: RoundedRectangle(cornerRadius: theme.shapes.radiusLg, style: .continuous),
                    // The hard shadow falls toward the trailing edge, which is the left in RTL.
                    offset: layoutDirection == .rightToLeft ? -theme.shapes.offsetShadow : theme.shapes.offsetShadow
                )
            }
    }
}

private struct CardBackground: View {
    let treatment: CardTreatment
    let shape: RoundedRectangle
    let offset: CGFloat

    @Environment(\.appTheme) private var theme

    var body: some View {
        switch treatment {
        case .outlined:
            shape
                .fill(theme.colors.surface)
                .overlay {
                    shape.strokeBorder(theme.colors.separator, lineWidth: theme.shapes.borderWidth)
                }
        case .raised:
            // The shadow belongs to the shape, not to the content, so it is drawn from a path.
            shape
                .fill(theme.colors.surfaceElevated)
                .shadow(color: theme.colors.shadow, radius: DesignTokens.Elevation.card, y: DesignTokens.Elevation.raised)
        case .offset:
            shape
                .fill(theme.colors.surface)
                .overlay {
                    shape.strokeBorder(theme.colors.contentPrimary, lineWidth: theme.shapes.borderWidthStrong)
                }
                .background {
                    shape
                        .fill(theme.colors.contentPrimary)
                        .offset(x: offset, y: abs(offset))
                }
        }
    }
}

#Preview("Cards") {
    VStack(spacing: AppSpacing.lg) {
        ForEach(AppDesignStyle.allCases, id: \.self) { style in
            AppCard {
                Text(verbatim: style.rawValue)
            }
            .appTheme(designStyle: style)
        }
    }
    .padding()
}
