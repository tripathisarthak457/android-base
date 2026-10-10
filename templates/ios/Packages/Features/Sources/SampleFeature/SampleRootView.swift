import DesignSystem
import Navigation
import Networking
import SampleData
import SwiftUI
// <opt:database>
import SwiftData
// </opt:database>

/// The Home tab, and the reference for a list-detail feature: the list beside the selected sample
/// in a wide window, and the sample pushed over the list on a phone.
///
/// What is selected lives in the tab's `Router`, so a deep link can select a sample, and tapping
/// the tab again goes back to the list. The selection alone decides what a phone shows: the
/// sample while one is selected, the list when none is. Binding `preferredCompactColumn` as well
/// would leave the phone on whichever column it last showed when the router changes the selection.
public struct SampleRootView: View {
    @Environment(Router.self) private var router
    @State private var listStore: SampleListStore
    private let repository: any SampleRepository

    // <opt:database>
    public init(client: APIClient, modelContainer: ModelContainer, monitor: NetworkMonitor, toasts: ToastCenter) {
        self.init(
            repository: DefaultSampleRepository(client: client, modelContainer: modelContainer),
            monitor: monitor,
            toasts: toasts
        )
    }
    // </opt:database>
    // <opt-else:database>
    //~ public init(client: APIClient, monitor: NetworkMonitor, toasts: ToastCenter) {
    //~     self.init(repository: DefaultSampleRepository(client: client), monitor: monitor, toasts: toasts)
    //~ }
    // </opt-else:database>

    init(repository: any SampleRepository, monitor: NetworkMonitor, toasts: ToastCenter) {
        self.repository = repository
        _listStore = State(initialValue: SampleListStore(
            repository: repository,
            connectivity: { monitor.updates() },
            toasts: toasts
        ))
    }
    // <opt:database>

    /// The model types this tab keeps on the device, for the app's one `ModelContainer`.
    public static var persistentModels: [any PersistentModel.Type] {
        SampleStorage.models
    }
    // </opt:database>

    public var body: some View {
        @Bindable var router = router
        NavigationSplitView {
            SampleListView(store: listStore, selection: selection)
        } detail: {
            NavigationStack(path: $router.path) {
                if let id = selectedID {
                    SampleDetailView(id: id, repository: repository)
                        .id(id)
                } else {
                    AppStateView(
                        Text("Nothing selected", bundle: .module),
                        systemImage: "doc.text",
                        message: Text("Choose something from the list to see it here.", bundle: .module)
                    )
                }
            }
        }
    }

    private var selectedID: SampleItem.ID? {
        router.selection.flatMap { Int($0) }
    }

    private var selection: Binding<SampleItem.ID?> {
        Binding {
            selectedID
        } set: { id in
            router.selection = id.map { String($0) }
        }
    }
}

#Preview("iPhone") {
    SampleRootView(repository: PreviewSampleRepository(), monitor: NetworkMonitor(), toasts: ToastCenter())
        .environment(Router())
        .appTheme()
}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {
    SampleRootView(repository: PreviewSampleRepository(), monitor: NetworkMonitor(), toasts: ToastCenter())
        .environment(Router())
        .appTheme(designStyle: .social)
}
