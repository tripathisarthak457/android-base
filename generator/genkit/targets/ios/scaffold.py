"""
The feature modules a spec names, scaffolded as a `<Name>Data` and a `<Name>Feature` target of the
Features package, with their sources, a store test and a String Catalog.

The output has the shape of the template's own features (STANDARDS-PLAN §2.12): a root view per
tab, one folder per screen with the store beside its view, the repository protocol and model under
`API/`. Text written here is final; the renamer never sees it.
"""

from __future__ import annotations

from pathlib import Path

from ...core.spec import Spec, module_title, pascal_case
from . import strings
from .tabs import tab_modules

_FEATURES = "Packages/Features"

#: The keys of a scaffolded list besides its title. Their translations are each language's
#: `_scaffold.json`.
_LIST_STRINGS = (
    "Nothing here yet",
    "When there is something to show, it will appear on this screen.",
    "Reload",
)


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
/// One item of {title}, as the app uses it. It decodes straight from the server's answer; give it
/// a DTO under `Remote/DTO/` once the wire format and the app's model differ.
public struct {pascal}Item: Identifiable, Hashable, Sendable, Decodable {{
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
}}
""")

    if spec.has("network"):
        repository = f"""\
import AppCore
import Networking

/// {title} from the backend, at `<API_BASE_URL>/{name}`.
public struct Default{pascal}Repository: {pascal}Repository {{
    private let client: APIClient

    public init(client: APIClient) {{
        self.client = client
    }}

    public func items() async -> AppResult<[{pascal}Item]> {{
        await client.send(Endpoint<[{pascal}Item]>.get("{name}"))
    }}
}}
"""
    else:
        repository = f"""\
import AppCore

/// Answers with no items: the project has no networking or database for it to read. Replace the
/// body of `items()` with the feature's real source.
public struct Default{pascal}Repository: {pascal}Repository {{
    public init() {{}}

    public func items() async -> AppResult<[{pascal}Item]> {{
        .success([])
    }}
}}
"""
    _write(root / f"Default{pascal}Repository.swift", repository)


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

/// The {title} tab: its list, and each item pushed over it.
public struct {pascal}RootView: View {{
    @Environment(Router.self) private var router
    @State private var store: {pascal}ListStore

{public_init}

    init(repository: any {pascal}Repository) {{
        _store = State(initialValue: {pascal}ListStore(repository: repository))
    }}

    public var body: some View {{
        @Bindable var router = router
        NavigationStack(path: $router.path) {{
            {pascal}ListView(store: store)
                .navigationDestination(for: {pascal}Route.self) {{ route in
                    switch route {{
                    case .detail(let item):
                        {pascal}DetailView(item: item)
                    }}
                }}
        }}
    }}
}}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {{
    {pascal}RootView(repository: Preview{pascal}Repository())
        .environment(Router())
        .appTheme()
}}
""")

    _write(root / f"{pascal}Route.swift", f"""\
import {pascal}Data

/// The screens of {title} that are pushed on its tab's stack.
nonisolated enum {pascal}Route: Hashable, Sendable {{
    case detail({pascal}Item)
}}
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

/// The {title.lower()} list, with every load state and pull to refresh.
struct {pascal}ListView: View {{
    let store: {pascal}ListStore

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
            List(store.state.items) {{ item in
                NavigationLink(value: {pascal}Route.detail(item)) {{
                    Text(item.title)
                        .font(theme.typography.body)
                        .foregroundStyle(theme.colors.contentPrimary)
                }}
            }}
            .refreshable {{ await store.refresh() }}
            .animation(theme.springs.layout, value: store.state.items)
        }}
    }}
}}

#Preview("iPhone") {{
    NavigationStack {{
        {pascal}ListView(store: {pascal}ListStore(repository: Preview{pascal}Repository()))
    }}
    .appTheme()
}}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {{
    NavigationStack {{
        {pascal}ListView(store: {pascal}ListStore(repository: Preview{pascal}Repository()))
    }}
    .appTheme()
}}
""")

    _write(root / "Detail" / f"{pascal}DetailView.swift", f"""\
import DesignSystem
import {pascal}Data
import SwiftUI

/// One item of {title}.
struct {pascal}DetailView: View {{
    let item: {pascal}Item

    @Environment(\\.appTheme) private var theme

    var body: some View {{
        ScrollView {{
            Text(item.title)
                .font(theme.typography.title2)
                .foregroundStyle(theme.colors.contentPrimary)
                .textSelection(.enabled)
                .frame(maxWidth: AppLayout.readableMaxWidth, alignment: .leading)
                .padding(AppSpacing.gutter)
                .frame(maxWidth: .infinity)
        }}
        .background(theme.colors.background)
        .navigationTitle(Text(item.title))
        .navigationBarTitleDisplayMode(.inline)
    }}
}}

#Preview("iPhone") {{
    NavigationStack {{
        {pascal}DetailView(item: Preview{pascal}Repository.examples[0])
    }}
    .appTheme()
}}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {{
    NavigationStack {{
        {pascal}DetailView(item: Preview{pascal}Repository.examples[0])
    }}
    .appTheme()
}}
""")

    _write(root / "Previews" / f"Preview{pascal}Repository.swift", f"""\
import AppCore
import {pascal}Data

/// Items for previews, answered at once and without the network.
nonisolated struct Preview{pascal}Repository: {pascal}Repository {{
    func items() async -> AppResult<[{pascal}Item]> {{
        .success(Self.examples)
    }}

    static let examples = [
        {pascal}Item(id: 1, title: "The first item"),
        {pascal}Item(id: 2, title: "A second item with a title long enough to wrap onto another line"),
        {pascal}Item(id: 3, title: "The third item"),
    ]
}}
""")

    _write(root / "Resources" / "Localizable.xcstrings", strings.render([title, *_LIST_STRINGS]))

    _write(tests / "List" / f"{pascal}ListStoreTests.swift", f"""\
import AppCore
import {pascal}Data
@testable import {pascal}Feature
import Testing

@MainActor
struct {pascal}ListStoreTests {{
    private let items = [{pascal}Item(id: 1, title: "One"), {pascal}Item(id: 2, title: "Two")]

    @Test func appearingLoadsTheItems() async {{
        let store = {pascal}ListStore(repository: Fake{pascal}Repository(answer: .success(items)))
        store.send(.appeared)
        await settle(store)
        #expect(store.state.load == .loaded)
        #expect(store.state.items == items)
    }}

    @Test func anEmptyAnswerIsEmpty() async {{
        let store = {pascal}ListStore(repository: Fake{pascal}Repository(answer: .success([])))
        store.send(.appeared)
        await settle(store)
        #expect(store.state.load == .empty)
    }}

    @Test func aFailedLoadShowsTheError() async {{
        let store = {pascal}ListStore(repository: Fake{pascal}Repository(answer: .failure(.offline)))
        store.send(.appeared)
        await settle(store)
        #expect(store.state.load == .failed(.offline))
    }}

    @Test func aFailedRefreshKeepsTheList() async {{
        let repository = Fake{pascal}Repository(answer: .success(items))
        let store = {pascal}ListStore(repository: repository)
        await store.refresh()
        await repository.answer(with: .failure(.timeout))

        await store.refresh()

        #expect(store.state.load == .loaded)
        #expect(store.state.items == items)
    }}

    /// Waits for the store's load to finish. The fake answers at once, but the answer still crosses
    /// from its actor to the main actor, so this polls rather than counting on a number of turns.
    private func settle(_ store: {pascal}ListStore) async {{
        let deadline = ContinuousClock.now + .seconds(2)
        while store.state.load == .idle || store.state.load == .loading, ContinuousClock.now < deadline {{
            try? await Task.sleep(for: .milliseconds(5))
        }}
    }}
}}

private actor Fake{pascal}Repository: {pascal}Repository {{
    private var answer: AppResult<[{pascal}Item]>

    init(answer: AppResult<[{pascal}Item]>) {{
        self.answer = answer
    }}

    func answer(with answer: AppResult<[{pascal}Item]>) {{
        self.answer = answer
    }}

    func items() async -> AppResult<[{pascal}Item]> {{
        answer
    }}
}}
""")


def _write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
