import AppCore
import DesignSystem
import SampleData
import SwiftUI

/// One sample, read in full, with its details in an inspector: a column beside it in a wide
/// window, a sheet on a phone.
struct SampleDetailView: View {
    @State private var store: SampleDetailStore
    /// Whether the person last opened or closed the details; nil until they do.
    @State private var chosenShowsDetails: Bool?

    @Environment(\.appTheme) private var theme
    @Environment(\.appWindowWidth) private var windowWidth

    init(id: SampleItem.ID, repository: any SampleRepository) {
        _store = State(initialValue: SampleDetailStore(id: id, repository: repository))
    }

    var body: some View {
        content
            .navigationTitle(Text("Detail", bundle: .module))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem {
                    Button {
                        chosenShowsDetails = !showsDetails.wrappedValue
                    } label: {
                        Label { Text("Details", bundle: .module) } icon: { Image(systemName: "info.circle") }
                    }
                    .keyboardShortcut("i", modifiers: .command)
                    .disabled(store.state.item == nil)
                }
            }
            .inspector(isPresented: showsDetails) {
                if let item = store.state.item {
                    // A column beside the article in a wide window, and a sheet on a phone.
                    SampleDetails(item: item)
                        .inspectorColumnWidth(
                            min: AppLayout.supportingPaneMinWidth,
                            ideal: AppLayout.supportingPaneIdealWidth,
                            max: AppLayout.supportingPaneMaxWidth
                        )
                        .presentationDetents([.medium, .large])
                }
            }
            .onAppear { store.send(.appeared) }
    }

    /// Until the person chooses, a large window shows the details beside the article, as the
    /// Android template shows its supporting pane at that width, and a narrower one leaves the
    /// article the whole width.
    private var showsDetails: Binding<Bool> {
        Binding {
            store.state.item != nil && (chosenShowsDetails ?? (windowWidth >= .large))
        } set: { shows in
            chosenShowsDetails = shows
        }
    }

    @ViewBuilder
    private var content: some View {
        switch store.state.load {
        case .idle, .loading:
            AppSkeleton {
                SampleArticle(item: SampleItem(
                    id: 0,
                    title: "A sample title that runs to a second line",
                    body: String(repeating: "A line of the sample's body text. ", count: 6)
                ))
            }
        case .failed(let error):
            AppFailureView(error) { store.send(.retry) }
        case .loaded, .refreshing, .empty:
            if let item = store.state.item {
                SampleArticle(item: item)
            }
        }
    }
}

private struct SampleArticle: View {
    let item: SampleItem

    @Environment(\.appTheme) private var theme

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.lg) {
                Text(item.title)
                    .font(theme.typography.title2)
                    .foregroundStyle(theme.colors.contentPrimary)
                    .textSelection(.enabled)
                AppCard {
                    Text(item.body)
                        .font(theme.typography.body)
                        .foregroundStyle(theme.colors.contentPrimary)
                        .textSelection(.enabled)
                }
            }
            .padding(AppSpacing.gutter)
            .frame(maxWidth: AppLayout.readableMaxWidth)
            .frame(maxWidth: .infinity)
        }
        .background(theme.colors.background)
    }
}

private struct SampleDetails: View {
    let item: SampleItem

    var body: some View {
        List {
            AppSection(Text("Details", bundle: .module)) {
                AppRow(Text("Number", bundle: .module)) {
                    Text(item.id, format: .number)
                }
                AppRow(Text("Words", bundle: .module)) {
                    Text(item.body.split(whereSeparator: \.isWhitespace).count, format: .number)
                }
            }
        }
        .appGroupedList()
    }
}

#if DEBUG
#Preview("iPhone") {
    NavigationStack {
        SampleDetailView(id: 1, repository: PreviewSampleRepository())
    }
    .appTheme()
}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {
    NavigationStack {
        SampleDetailView(id: 2, repository: PreviewSampleRepository())
    }
    .appTheme(designStyle: .editorial)
}
#endif
