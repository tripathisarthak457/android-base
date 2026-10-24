import SwiftUI

/// A section of an inset-grouped list, with the design style's row background and header case.
///
/// Use it inside a `List` styled with `appGroupedList()`.
public struct AppSection<Content: View, Footer: View>: View {
    private let title: Text?
    private let content: Content
    private let footer: Footer

    @Environment(\.appTheme) private var theme

    public init(_ title: Text? = nil, @ViewBuilder content: () -> Content, @ViewBuilder footer: () -> Footer) {
        self.title = title
        self.content = content()
        self.footer = footer()
    }

    public var body: some View {
        Section {
            content
                .listRowBackground(theme.colors.surfaceGrouped)
        } header: {
            if let title {
                title
                    .font(theme.typography.footnote)
                    .foregroundStyle(theme.colors.contentSecondary)
                    .textCase(theme.shapes.uppercaseLabels ? .uppercase : nil)
            }
        } footer: {
            footer
                .font(theme.typography.footnote)
                .foregroundStyle(theme.colors.contentSecondary)
        }
    }
}

extension AppSection where Footer == EmptyView {
    public init(_ title: Text? = nil, @ViewBuilder content: () -> Content) {
        self.init(title, content: content) {
            EmptyView()
        }
    }
}
