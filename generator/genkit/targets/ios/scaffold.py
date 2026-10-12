"""
The feature modules a spec names, scaffolded as a `<Name>Data` and a `<Name>Feature` target of the
Features package, with their sources, store tests and a String Catalog.

Each module is a list-detail feature in the shape of the template's `SampleFeature` (ADAPTIVE-PLAN
§14.2, STANDARDS-PLAN §2.12): a split view whose selection is the tab router's, so a wide window
shows the list beside the item and a phone pushes the item; the detail loads by id, so a deep
link opens an item the list has not loaded. One folder per screen with the store beside its view;
the repository protocol and model under `API/`, the wire format under `Remote/`. Text written here
is final; the renamer never sees it.
"""

from __future__ import annotations

from pathlib import Path

from ...core.spec import Spec, module_title, pascal_case
from . import strings
from .tabs import tab_modules

_FEATURES = "Packages/Features"

#: The keys of a scaffolded module besides its title. Their translations are each language's
#: `_scaffold.json`.
STRINGS = (
    "Nothing here yet",
    "When there is something to show, it will appear on this screen.",
    "Reload",
    "Nothing selected",
    "Choose something from the list to see it here.",
)


#: Every scaffolded test target's `waitUntil`, the same as `SampleFeatureTests/WaitUntil.swift`:
#: test targets cannot share a source file, and one helper does not earn a test-support product.
WAIT_UNTIL = """\
import Observation

/// Returns once `condition` holds, checking it again each time something it reads changes.
///
/// It waits on the store's own changes instead of polling the clock, so a slow machine cannot fail
/// a test; the suite's time limit ends a wait for a change that never comes.
@MainActor
func waitUntil(_ condition: () -> Bool) async {
    while !condition() {
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            withObservationTracking {
                _ = condition()
            } onChange: {
                continuation.resume()
            }
        }
    }
}
"""


def regions(spec: Spec) -> dict[str, list[str] | None]:
    modules = tab_modules(spec)
    return {
        "feature-products": [
            f'        .library(name: "{pascal_case(name)}Feature", targets: ["{pascal_case(name)}Feature"]),\n'
            for name in modules
        ],
        "feature-targets": [line for name in modules for line in _package_targets(spec, name)],
    }


def feature_products(spec: Spec) -> list[str]:
    """The Features package's products the app links, in tab order, then the screens that are not tabs."""
    products = ["SampleFeature"] if spec.has("sample") else []
    products += feature_targets(spec)
    products += [name for key, name in (("settings", "SettingsFeature"), ("onboarding", "OnboardingFeature")) if spec.has(key)]
    return products


def feature_targets(spec: Spec) -> list[str]:
    """The scaffolded `<Name>Feature` targets."""
    return [f"{pascal_case(name)}Feature" for name in tab_modules(spec)]


def user_names(spec: Spec) -> dict[str, set[str]]:
    """
    Per String Catalog, the keys that are a module's name as the user typed it: its tab title in the
    app's catalog and its screen title in its own. No translation file can know them.
    """
    titles = {module_title(name) for name in tab_modules(spec)}
    return {"app": titles} | {
        f"{pascal_case(name)}Feature": {module_title(name)} for name in tab_modules(spec)
    }


def write_feature_modules(project: Path, spec: Spec) -> None:
    for name in tab_modules(spec):
        _write_data(project, spec, name)
        _write_feature(project, spec, name)


def _package_targets(spec: Spec, name: str) -> list[str]:
    pascal = pascal_case(name)
    networking = [_core("Networking")] if spec.has("network") else []
    lines = [
        "        .target(",
        f'            name: "{pascal}Data",',
        "            dependencies: [",
        _core("AppCore"),
        *networking,
        "            ],",
        "            swiftSettings: dataSettings",
        "        ),",
        "        .target(",
        f'            name: "{pascal}Feature",',
        "            dependencies: [",
        f'                "{pascal}Data",',
        _core("AppCore"),
        _core("DesignSystem"),
        _core("Navigation"),
        *networking,
        "            ],",
        '            resources: [.process("Resources")],',
        "            swiftSettings: featureSettings",
        "        ),",
        "        .testTarget(",
        f'            name: "{pascal}FeatureTests",',
        "            dependencies: [",
        f'                "{pascal}Feature",',
        f'                "{pascal}Data",',
        _core("AppCore"),
        "            ],",
        "            swiftSettings: featureSettings",
        "        ),",
    ]
    return [f"{line}\n" for line in lines]


def _core(product: str) -> str:
    return f'                .product(name: "{product}", package: "Core"),'


def _write_data(project: Path, spec: Spec, name: str) -> None:
    pascal = pascal_case(name)
    title = module_title(name)
    root = project / _FEATURES / "Sources" / f"{pascal}Data"

    _write(root / "API" / f"{pascal}Item.swift", f"""\
/// One item of {title}, as the app uses it.
public struct {pascal}Item: Identifiable, Hashable, Sendable {{
    public let id: Int
    public let title: String

    public init(id: Int, title: String) {{
        self.id = id
        self.title = title
    }}
}}
""")

    _write(root / "API" / f"{pascal}Repository.swift", f"""\
import AppCore

/// Everything the app can ask about {title.lower()}. A protocol, so a store's tests use a fake.
public protocol {pascal}Repository: Sendable {{
    func items() async -> AppResult<[{pascal}Item]>

    func item(id: {pascal}Item.ID) async -> AppResult<{pascal}Item>
}}
""")

    if not spec.has("network"):
        _write(root / f"Default{pascal}Repository.swift", f"""\
import AppCore

/// {title} held in memory, empty to start with: the project has no networking for it to read.
/// Replace it with the feature's real source.
public struct Default{pascal}Repository: {pascal}Repository {{
    private let saved: [{pascal}Item]

    public init(items: [{pascal}Item] = []) {{
        saved = items
    }}

    public func items() async -> AppResult<[{pascal}Item]> {{
        .success(saved)
    }}

    public func item(id: {pascal}Item.ID) async -> AppResult<{pascal}Item> {{
        guard let item = saved.first(where: {{ $0.id == id }}) else {{
            return .failure(.http(HTTPFailure(status: 404)))
        }}
        return .success(item)
    }}
}}
""")
        return

    _write(root / "Remote" / "DTO" / f"{pascal}ItemDTO.swift", f"""\
/// One item of {title} exactly as the server sends it.
struct {pascal}ItemDTO: Decodable, Sendable {{
    let id: Int
    let title: String

    var item: {pascal}Item {{
        {pascal}Item(id: id, title: title)
    }}
}}
""")

    _write(root / "Remote" / f"{pascal}API.swift", f"""\
import AppCore
import Networking

/// The {title.lower()} endpoints, at `<API_BASE_URL>/{name}`.
///
/// `@concurrent`, so decoding and mapping run off the caller's actor, which for a store is the
/// main actor.
struct {pascal}API: Sendable {{
    let client: APIClient

    @concurrent
    func items() async -> AppResult<[{pascal}Item]> {{
        await client.send(Endpoint<[{pascal}ItemDTO]>.get("{name}")).map {{ dtos in dtos.map(\\.item) }}
    }}

    @concurrent
    func item(id: {pascal}Item.ID) async -> AppResult<{pascal}Item> {{
        await client.send(Endpoint<{pascal}ItemDTO>.get("{name}/\\(id)")).map(\\.item)
    }}
}}
""")

    _write(root / f"Default{pascal}Repository.swift", f"""\
import AppCore
import Networking

/// {title} from the backend.
public struct Default{pascal}Repository: {pascal}Repository {{
    private let api: {pascal}API

    public init(client: APIClient) {{
        api = {pascal}API(client: client)
    }}

    public func items() async -> AppResult<[{pascal}Item]> {{
        await api.items()
    }}

    public func item(id: {pascal}Item.ID) async -> AppResult<{pascal}Item> {{
        await api.item(id: id)
    }}
}}
""")


def _write_feature(project: Path, spec: Spec, name: str) -> None:
    pascal = pascal_case(name)
    title = module_title(name)
    root = project / _FEATURES / "Sources" / f"{pascal}Feature"
    tests = project / _FEATURES / "Tests" / f"{pascal}FeatureTests"
    network = spec.has("network")
    networking_import = "import Networking\n" if network else ""

    if network:
        public_init = f"""\
    public init(client: APIClient) {{
        self.init(repository: Default{pascal}Repository(client: client))
    }}"""
    else:
        public_init = f"""\
    public init() {{
        self.init(repository: Default{pascal}Repository())
    }}"""
    _write(root / f"{pascal}RootView.swift", f"""\
import DesignSystem
import Navigation
{networking_import}import {pascal}Data
import SwiftUI

/// The {title} tab.
///
/// A wide window shows the list beside the selected item, and a phone pushes the item over the
/// list. What is selected lives in the tab's `Router`, so a deep link opens an item and tapping
/// the tab again goes back to the list.
public struct {pascal}RootView: View {{
    @Environment(Router.self) private var router
    @State private var listStore: {pascal}ListStore
    private let repository: any {pascal}Repository

{public_init}

    init(repository: any {pascal}Repository) {{
        self.repository = repository
        _listStore = State(initialValue: {pascal}ListStore(repository: repository))
    }}

    public var body: some View {{
        @Bindable var router = router
        NavigationSplitView {{
            {pascal}ListView(store: listStore, selection: selection)
        }} detail: {{
            NavigationStack(path: $router.path) {{
                if let id = selectedID {{
                    {pascal}DetailView(id: id, repository: repository)
                        .id(id)
                }} else {{
                    AppStateView(
                        Text("Nothing selected", bundle: .module),
                        systemImage: "doc.text",
                        message: Text("Choose something from the list to see it here.", bundle: .module)
                    )
                }}
            }}
        }}
    }}

    private var selectedID: {pascal}Item.ID? {{
        router.selection.flatMap {{ Int($0) }}
    }}

    private var selection: Binding<{pascal}Item.ID?> {{
        Binding {{
            selectedID
        }} set: {{ id in
            router.selection = id.map {{ String($0) }}
        }}
    }}
}}

#if DEBUG
#Preview("iPhone") {{
    {pascal}RootView(repository: Preview{pascal}Repository())
        .environment(Router())
        .appTheme()
}}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {{
    {pascal}RootView(repository: Preview{pascal}Repository())
        .environment(Router())
        .appTheme()
}}
#endif
""")

    _write(root / "List" / f"{pascal}ListStore.swift", f"""\
import AppCore
import Observation
import {pascal}Data

/// The {title.lower()} list: loading it, and loading it again.
@MainActor
@Observable
final class {pascal}ListStore {{
    struct State: Equatable, Sendable {{
        var load: LoadState = .idle
        var items: [{pascal}Item] = []
    }}

    enum Event: Sendable {{
        case appeared
        case retry
    }}

    private(set) var state = State()

    private let repository: any {pascal}Repository
    private let tasks = TaskBag()

    init(repository: any {pascal}Repository) {{
        self.repository = repository
    }}

    func send(_ event: Event) {{
        switch event {{
        case .appeared:
            if state.load == .idle {{
                load(refreshing: false)
            }}
        case .retry:
            load(refreshing: false)
        }}
    }}

    /// Reloads for pull to refresh, and returns once the reload is done, so the spinner stays
    /// exactly as long as the work.
    func refresh() async {{
        await load(refreshing: true).value
    }}

    @discardableResult
    private func load(refreshing: Bool) -> Task<Void, Never> {{
        tasks.latest(Self.loading) {{ [weak self] in
            await self?.performLoad(refreshing: refreshing)
        }}
    }}

    private func performLoad(refreshing: Bool) async {{
        state.load = refreshing ? .refreshing : .loading
        let result = await repository.items()
        guard !Task.isCancelled else {{ return }}

        switch result {{
        case .success(let items):
            state.items = items
            state.load = items.isEmpty ? .empty : .loaded
        case .failure where refreshing && !state.items.isEmpty:
            // The list on screen is still worth reading, so a failed refresh keeps it.
            state.load = .loaded
        case .failure(let error):
            state.load = .failed(error)
        }}
    }}

    private static let loading = "load"
}}
""")

    _write(root / "List" / f"{pascal}ListView.swift", f"""\
import AppCore
import DesignSystem
import {pascal}Data
import SwiftUI

/// The {title.lower()} list column, with every load state and pull to refresh.
struct {pascal}ListView: View {{
    let store: {pascal}ListStore
    @Binding var selection: {pascal}Item.ID?

    @Environment(\\.appTheme) private var theme

    var body: some View {{
        content
            .navigationTitle(Text("{title}", bundle: .module))
            .onAppear {{ store.send(.appeared) }}
    }}

    @ViewBuilder
    private var content: some View {{
        switch store.state.load {{
        case .idle, .loading:
            AppSkeleton {{
                List(0..<8, id: \\.self) {{ _ in
                    Text(verbatim: "A title of a typical length")
                }}
            }}
        case .failed(let error):
            AppFailureView(error) {{ store.send(.retry) }}
        case .empty:
            AppStateView(
                Text("Nothing here yet", bundle: .module),
                systemImage: "tray",
                message: Text("When there is something to show, it will appear on this screen.", bundle: .module)
            ) {{
                Button {{ store.send(.retry) }} label: {{ Text("Reload", bundle: .module) }}
                    .buttonStyle(.app(.tinted))
            }}
        case .loaded, .refreshing:
            List(selection: $selection) {{
                ForEach(store.state.items) {{ item in
                    Text(item.title)
                        .font(theme.typography.body)
                        .foregroundStyle(theme.colors.contentPrimary)
                        .tag(item.id)
                }}
            }}
            .refreshable {{ await store.refresh() }}
            .animation(theme.springs.layout, value: store.state.items)
        }}
    }}
}}

#if DEBUG
#Preview("iPhone") {{
    NavigationStack {{
        {pascal}ListView(store: {pascal}ListStore(repository: Preview{pascal}Repository()), selection: .constant(nil))
    }}
    .appTheme()
}}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {{
    NavigationSplitView {{
        {pascal}ListView(store: {pascal}ListStore(repository: Preview{pascal}Repository()), selection: .constant(2))
    }} detail: {{
        Text(verbatim: "Detail")
    }}
    .appTheme()
}}
#endif
""")

    _write(root / "Detail" / f"{pascal}DetailStore.swift", f"""\
import AppCore
import Observation
import {pascal}Data

/// One item of {title}, loaded by id, so a deep link can open an item the list has not loaded.
@MainActor
@Observable
final class {pascal}DetailStore {{
    struct State: Equatable, Sendable {{
        var load: LoadState = .idle
        var item: {pascal}Item?
    }}

    enum Event: Sendable {{
        case appeared
        case retry
    }}

    private(set) var state = State()

    private let id: {pascal}Item.ID
    private let repository: any {pascal}Repository
    private let tasks = TaskBag()

    init(id: {pascal}Item.ID, repository: any {pascal}Repository) {{
        self.id = id
        self.repository = repository
    }}

    func send(_ event: Event) {{
        switch event {{
        case .appeared:
            if state.load == .idle {{
                load()
            }}
        case .retry:
            load()
        }}
    }}

    private func load() {{
        tasks.latest(Self.loading) {{ [weak self] in
            await self?.performLoad()
        }}
    }}

    private func performLoad() async {{
        state.load = .loading
        let result = await repository.item(id: id)
        guard !Task.isCancelled else {{ return }}

        switch result {{
        case .success(let item):
            state.item = item
            state.load = .loaded
        case .failure(let error):
            state.load = .failed(error)
        }}
    }}

    private static let loading = "load"
}}
""")

    _write(root / "Detail" / f"{pascal}DetailView.swift", f"""\
import DesignSystem
import {pascal}Data
import SwiftUI

/// One item of {title}, read in full.
struct {pascal}DetailView: View {{
    @State private var store: {pascal}DetailStore

    init(id: {pascal}Item.ID, repository: any {pascal}Repository) {{
        _store = State(initialValue: {pascal}DetailStore(id: id, repository: repository))
    }}

    var body: some View {{
        content
            .onAppear {{ store.send(.appeared) }}
    }}

    @ViewBuilder
    private var content: some View {{
        switch store.state.load {{
        case .idle, .loading:
            AppSkeleton {{
                {pascal}Article(title: "A title of a typical length")
            }}
        case .failed(let error):
            AppFailureView(error) {{ store.send(.retry) }}
        case .loaded, .refreshing, .empty:
            if let item = store.state.item {{
                {pascal}Article(title: item.title)
            }}
        }}
    }}
}}

private struct {pascal}Article: View {{
    let title: String

    @Environment(\\.appTheme) private var theme

    var body: some View {{
        ScrollView {{
            Text(title)
                .font(theme.typography.title2)
                .foregroundStyle(theme.colors.contentPrimary)
                .textSelection(.enabled)
                .frame(maxWidth: AppLayout.readableMaxWidth, alignment: .leading)
                .padding(AppSpacing.gutter)
                .frame(maxWidth: .infinity)
        }}
        .background(theme.colors.background)
    }}
}}

#if DEBUG
#Preview("iPhone") {{
    NavigationStack {{
        {pascal}DetailView(id: 1, repository: Preview{pascal}Repository())
    }}
    .appTheme()
}}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {{
    NavigationStack {{
        {pascal}DetailView(id: 2, repository: Preview{pascal}Repository())
    }}
    .appTheme()
}}
#endif
""")

    _write(root / "Previews" / f"Preview{pascal}Repository.swift", f"""\
import AppCore
import {pascal}Data

#if DEBUG
/// Items for previews, answered at once and without the network.
nonisolated struct Preview{pascal}Repository: {pascal}Repository {{
    func items() async -> AppResult<[{pascal}Item]> {{
        .success(Self.examples)
    }}

    func item(id: {pascal}Item.ID) async -> AppResult<{pascal}Item> {{
        guard let item = Self.examples.first(where: {{ $0.id == id }}) else {{
            return .failure(.http(HTTPFailure(status: 404)))
        }}
        return .success(item)
    }}

    static let examples = [
        {pascal}Item(id: 1, title: "The first item"),
        {pascal}Item(id: 2, title: "A second item with a title long enough to wrap onto another line"),
        {pascal}Item(id: 3, title: "The third item"),
    ]
}}
#endif
""")

    _write(root / "Resources" / "Localizable.xcstrings", strings.render([title, *STRINGS]))

    _write(tests / f"Fake{pascal}Repository.swift", f"""\
import AppCore
import {pascal}Data

/// Answers with whatever the test last told it to, and counts the questions.
actor Fake{pascal}Repository: {pascal}Repository {{
    private var answer: AppResult<[{pascal}Item]>
    private(set) var requests = 0

    init(answer: AppResult<[{pascal}Item]>) {{
        self.answer = answer
    }}

    func answer(with answer: AppResult<[{pascal}Item]>) {{
        self.answer = answer
    }}

    func items() async -> AppResult<[{pascal}Item]> {{
        requests += 1
        return answer
    }}

    func item(id: {pascal}Item.ID) async -> AppResult<{pascal}Item> {{
        switch answer {{
        case .success(let items):
            guard let item = items.first(where: {{ $0.id == id }}) else {{
                return .failure(.http(HTTPFailure(status: 404)))
            }}
            return .success(item)
        case .failure(let error):
            return .failure(error)
        }}
    }}

    static let items = [{pascal}Item(id: 1, title: "One"), {pascal}Item(id: 2, title: "Two")]
}}
""")

    _write(tests / "WaitUntil.swift", WAIT_UNTIL)

    _write(tests / "List" / f"{pascal}ListStoreTests.swift", f"""\
import AppCore
import {pascal}Data
@testable import {pascal}Feature
import Testing

@MainActor
@Suite(.timeLimit(.minutes(1)))
struct {pascal}ListStoreTests {{
    @Test func appearingLoadsTheItems() async {{
        let store = {pascal}ListStore(repository: Fake{pascal}Repository(answer: .success(Fake{pascal}Repository.items)))
        store.send(.appeared)
        await waitUntil {{ store.state.load == .loaded }}
        #expect(store.state.items == Fake{pascal}Repository.items)
    }}

    @Test func anEmptyAnswerIsEmpty() async {{
        let store = {pascal}ListStore(repository: Fake{pascal}Repository(answer: .success([])))
        store.send(.appeared)
        await waitUntil {{ store.state.load == .empty }}
        #expect(store.state.items.isEmpty)
    }}

    @Test func aFailedLoadShowsTheError() async {{
        let store = {pascal}ListStore(repository: Fake{pascal}Repository(answer: .failure(.offline)))
        store.send(.appeared)
        await waitUntil {{ store.state.load == .failed(.offline) }}
        #expect(store.state.items.isEmpty)
    }}

    @Test func aFailedRefreshKeepsTheList() async {{
        let repository = Fake{pascal}Repository(answer: .success(Fake{pascal}Repository.items))
        let store = {pascal}ListStore(repository: repository)
        await store.refresh()
        await repository.answer(with: .failure(.timeout))

        await store.refresh()

        #expect(store.state.load == .loaded)
        #expect(store.state.items == Fake{pascal}Repository.items)
    }}
}}
""")

    _write(tests / "Detail" / f"{pascal}DetailStoreTests.swift", f"""\
import AppCore
import {pascal}Data
@testable import {pascal}Feature
import Testing

@MainActor
@Suite(.timeLimit(.minutes(1)))
struct {pascal}DetailStoreTests {{
    @Test func loadsTheItemItWasOpenedFor() async {{
        let store = {pascal}DetailStore(id: 2, repository: Fake{pascal}Repository(answer: .success(Fake{pascal}Repository.items)))
        store.send(.appeared)
        await waitUntil {{ store.state.load == .loaded }}
        #expect(store.state.item == Fake{pascal}Repository.items[1])
    }}

    @Test func aMissingItemIsAFailure() async {{
        let store = {pascal}DetailStore(id: 99, repository: Fake{pascal}Repository(answer: .success(Fake{pascal}Repository.items)))
        store.send(.appeared)
        await waitUntil {{ store.state.load != .idle && store.state.load != .loading }}
        #expect(store.state.load == .failed(.http(HTTPFailure(status: 404))))
    }}
}}
""")


def _write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
