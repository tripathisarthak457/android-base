import AppCore
import DesignSystem
import Foundation
import Networking
import Observation
import SampleData

/// The samples list: loading, searching, refreshing, and reloading when the connection comes back
/// after a load failed for want of one.
@MainActor
@Observable
final class SampleListStore {
    struct State: Equatable, Sendable {
        var load: LoadState = .idle
        var items: [SampleItem] = []
        var query = ""
        /// `items` filtered by `query`, kept here so the list does not filter on every redraw.
        var visibleItems: [SampleItem] = []
        var isOnline = true
    }

    enum Event: Sendable {
        case appeared
        case retry
        case queryChanged(String)
    }

    private(set) var state = State()

    private let repository: any SampleRepository
    private let monitor: NetworkMonitor
    private let toasts: ToastCenter
    private let tasks = TaskBag()

    init(repository: any SampleRepository, monitor: NetworkMonitor, toasts: ToastCenter) {
        self.repository = repository
        self.monitor = monitor
        self.toasts = toasts
    }

    func send(_ event: Event) {
        switch event {
        case .appeared:
            if state.load == .idle {
                load(refreshing: false)
            }
            followConnectivity()
        case .retry:
            load(refreshing: false)
        case .queryChanged(let query):
            state.query = query
            state.visibleItems = Self.matching(state.items, query)
        }
    }

    /// Reloads for pull to refresh, and returns once the reload is done, so the spinner stays
    /// exactly as long as the work.
    func refresh() async {
        await load(refreshing: true).value
    }

    @discardableResult
    private func load(refreshing: Bool) -> Task<Void, Never> {
        tasks.latest(Self.loading) { [weak self] in
            await self?.performLoad(refreshing: refreshing)
        }
    }

    private func performLoad(refreshing: Bool) async {
        state.load = refreshing ? .refreshing : .loading
        let result = await repository.items(forceRefresh: refreshing)
        guard !Task.isCancelled else { return }

        switch result {
        case .success(let items):
            state.items = items
            state.visibleItems = Self.matching(items, state.query)
            state.load = items.isEmpty ? .empty : .loaded
        case .failure where refreshing && !state.items.isEmpty:
            // The list on screen is still worth reading, so it stays, and a toast says why it is
            // not newer.
            state.load = .loaded
            toasts.show(Toast(String(localized: "Could not refresh.", bundle: .module), kind: .error))
        case .failure(let error):
            state.load = .failed(error)
        }
    }

    private func followConnectivity() {
        tasks.exclusive(Self.connectivity) { [weak self, monitor] in
            for await isOnline in monitor.updates() {
                guard let self else { return }
                let reconnected = isOnline && !state.isOnline
                state.isOnline = isOnline
                if reconnected, state.load == .failed(.offline) {
                    load(refreshing: false)
                }
            }
        }
    }

    private static func matching(_ items: [SampleItem], _ query: String) -> [SampleItem] {
        let wanted = query.trimmingCharacters(in: .whitespaces)
        return wanted.isEmpty ? items : items.filter { $0.title.localizedStandardContains(wanted) }
    }

    private static let loading = "load"
    private static let connectivity = "connectivity"
}
