import SwiftUI

/// A list row: an optional symbol, a title, an optional subtitle and a trailing accessory such as
/// a value, a toggle or a chevron.
///
/// The row lays out its content only. Inside a `List`, wrap it in a `Button` or a `NavigationLink`
/// to make it tappable, and the system highlights it; outside one, use `.buttonStyle(.highlightRow)`.
public struct AppRow<Accessory: View>: View {
    private let title: Text
    private let subtitle: Text?
    private let systemImage: String?
    private let accessory: Accessory

    @Environment(\.appTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var symbolWidth = AppSizes.iconLarge

    public init(
        _ title: Text,
        subtitle: Text? = nil,
        systemImage: String? = nil,
        @ViewBuilder accessory: () -> Accessory
    ) {
        self.title = title
        self.subtitle = subtitle
        self.systemImage = systemImage
        self.accessory = accessory()
    }

    public var body: some View {
        HStack(spacing: AppSpacing.md) {
            if let systemImage {
                Image(systemName: systemImage)
                    .font(theme.typography.body)
                    .foregroundStyle(theme.colors.accentContent)
                    .frame(width: symbolWidth)
                    .accessibilityHidden(true)
            }
            VStack(alignment: .leading, spacing: AppSpacing.xxs) {
                title
                    .font(theme.typography.body)
                    .foregroundStyle(theme.colors.contentPrimary)
                if let subtitle {
                    subtitle
                        .font(theme.typography.subheadline)
                        .foregroundStyle(theme.colors.contentSecondary)
                }
            }
            Spacer(minLength: AppSpacing.sm)
            accessory
                .font(theme.typography.body)
                .foregroundStyle(theme.colors.contentSecondary)
        }
        .frame(minHeight: AppSizes.minTouchTarget)
        .accessibilityElement(children: .combine)
    }
}

extension AppRow where Accessory == EmptyView {
    public init(_ title: Text, subtitle: Text? = nil, systemImage: String? = nil) {
        self.init(title, subtitle: subtitle, systemImage: systemImage) {
            EmptyView()
        }
    }
}
