import SwiftUI

extension View {
    /// Styles a `List` as the design style's inset-grouped list: sections of `surfaceGrouped` on
    /// the `backgroundGrouped` page, as Settings looks.
    public func appGroupedList() -> some View {
        modifier(AppGroupedListModifier())
    }
}

private struct AppGroupedListModifier: ViewModifier {
    @Environment(\.appTheme) private var theme

    func body(content: Content) -> some View {
        content
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(theme.colors.backgroundGrouped)
    }
}

#Preview("Grouped list") {
    List {
        AppSection(Text(verbatim: "Appearance")) {
            AppRow(Text(verbatim: "Theme"), systemImage: "circle.lefthalf.filled") {
                Text(verbatim: "System")
            }
            AppRow(Text(verbatim: "Haptics"), subtitle: Text(verbatim: "Selection ticks and outcomes"))
        }
    }
    .appGroupedList()
    .appTheme()
}
