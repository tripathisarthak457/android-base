import AppCore
import DesignSystem
import SampleData
import SwiftUI

/// The list column: every load state, search, pull to refresh, and a context menu with a preview.
struct SampleListView: View {
    let store: SampleListStore
    @Binding var selection: SampleItem.ID?

    @Environment(\.appTheme) private var theme

    var body: some View {
        content
            .navigationTitle(Text("Samples", bundle: .module))
            .searchable(text: query, prompt: Text("Search titles", bundle: .module))
            .safeAreaInset(edge: .top) {
                if !store.state.isOnline, store.state.load.hasContent {
                    OfflineBanner()
                }
            }
            .onAppear { store.send(.appeared) }
    }

    @ViewBuilder
    private var content: some View {
        switch store.state.load {
        case .idle, .loading:
            SampleListSkeleton()
        case .failed(let error):
            AppFailureView(error) { store.send(.retry) }
        case .empty:
            AppStateView(
                Text("Nothing here yet", bundle: .module),
                systemImage: "tray",
                message: Text("When there is something to show, it will appear on this screen.", bundle: .module)
            ) {
                Button { store.send(.retry) } label: { Text("Reload", bundle: .module) }
                    .buttonStyle(.app(.tinted))
            }
        case .loaded, .refreshing:
            list
        }
    }

    private var list: some View {
        List(selection: $selection) {
            ForEach(store.state.visibleItems) { item in
                SampleRow(item: item)
                    .tag(item.id)
                    .draggable(item.body)
                    .contextMenu {
                        Button {
                            selection = item.id
                        } label: {
                            Label { Text("Open", bundle: .module) } icon: { Image(systemName: "doc.text") }
                        }
                        ShareLink(item: item.body, subject: Text(item.title)) {
                            Label { Text("Share", bundle: .module) } icon: { Image(systemName: "square.and.arrow.up") }
                        }
                    } preview: {
                        SamplePreviewCard(item: item)
                    }
            }
        }
        .refreshable { await store.refresh() }
        .overlay {
            if store.state.visibleItems.isEmpty, !store.state.query.isEmpty {
                ContentUnavailableView.search(text: store.state.query)
            }
        }
        .animation(theme.springs.layout, value: store.state.visibleItems)
    }

    private var query: Binding<String> {
        Binding {
            store.state.query
        } set: { query in
            store.send(.queryChanged(query))
        }
    }
}

private struct SampleListSkeleton: View {
    var body: some View {
        AppSkeleton {
            List(0..<8, id: \.self) { _ in
                SampleRow(item: SampleItem(
                    id: 0,
                    title: "A sample title that runs to a second line",
                    body: "The first words of its body, as the list shows them under the title."
                ))
            }
        }
    }
}

private struct OfflineBanner: View {
    @Environment(\.appTheme) private var theme

    var body: some View {
        Label {
            Text("Offline", bundle: .module)
        } icon: {
            Image(systemName: "wifi.slash")
        }
        .font(theme.typography.footnote.weight(.semibold))
        .foregroundStyle(theme.colors.warning.content)
        .padding(.horizontal, AppSpacing.md)
        .padding(.vertical, AppSpacing.xs)
        .background(theme.colors.warning.subtle, in: Capsule())
        .padding(.vertical, AppSpacing.xs)
        .accessibilityAddTraits(.isStaticText)
    }
}

/// What a long press or a right click lifts out of the list: the whole sample, readable.
private struct SamplePreviewCard: View {
    let item: SampleItem

    @Environment(\.appTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.sm) {
            Text(item.title)
                .font(theme.typography.headline)
                .foregroundStyle(theme.colors.contentPrimary)
            Text(item.body)
                .font(theme.typography.body)
                .foregroundStyle(theme.colors.contentSecondary)
        }
        .padding(AppSpacing.lg)
        .frame(idealWidth: AppLayout.minimumWindowWidth, alignment: .leading)
        .background(theme.colors.surfaceElevated)
    }
}

#Preview("iPhone") {
    NavigationStack {
        SampleListView(store: .preview(), selection: .constant(nil))
    }
    .appTheme()
}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {
    NavigationSplitView {
        SampleListView(store: .preview(), selection: .constant(2))
    } detail: {
        Text(verbatim: "Detail")
    }
    .appTheme()
}
