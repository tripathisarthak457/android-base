import SwiftUI

extension View {
    /// Presents `content` as a sheet with the design system's presentation: the given detents
    /// (medium and large by default, with the pages behind stepping back as a card stack), a
    /// grabber, and the design style's corner radius before iOS 26, whose sheets take the system's
    /// concentric corners.
    public func appSheet<Content: View>(
        isPresented: Binding<Bool>,
        detents: Set<PresentationDetent> = [.medium, .large],
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        sheet(isPresented: isPresented) {
            AppSheetContent(detents: detents, content: content())
        }
    }

    /// Presents a sheet for `item` while it is not nil, as `appSheet(isPresented:)` does.
    public func appSheet<Item: Identifiable, Content: View>(
        item: Binding<Item?>,
        detents: Set<PresentationDetent> = [.medium, .large],
        @ViewBuilder content: @escaping (Item) -> Content
    ) -> some View {
        sheet(item: item) { item in
            AppSheetContent(detents: detents, content: content(item))
        }
    }
}

private struct AppSheetContent<Content: View>: View {
    let detents: Set<PresentationDetent>
    let content: Content

    @Environment(\.appTheme) private var theme

    var body: some View {
        if #available(iOS 26, *) {
            presented
        } else {
            presented.presentationCornerRadius(theme.shapes.radiusSheet)
        }
    }

    private var presented: some View {
        content
            .presentationDetents(detents)
            .presentationDragIndicator(.visible)
    }
}

#Preview("Sheet") {
    @Previewable @State var isPresented = true
    Button {
        isPresented = true
    } label: {
        Text(verbatim: "Show the sheet")
    }
    .appSheet(isPresented: $isPresented) {
        Text(verbatim: "Drag the grabber between the detents.")
            .padding()
    }
    .appTheme()
}
