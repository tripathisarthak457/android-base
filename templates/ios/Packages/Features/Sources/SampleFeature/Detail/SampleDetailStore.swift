import AppCore
import Observation
import SampleData

/// One sample, loaded by id, so a deep link can open a sample the list has not loaded yet.
@MainActor
@Observable
final class SampleDetailStore {
    struct State: Equatable, Sendable {
        var load: LoadState = .idle
        var item: SampleItem?
    }

    enum Event: Sendable {
        case appeared
        case retry
    }

    private(set) var state = State()

    private let id: SampleItem.ID
    private let repository: any SampleRepository
    private let tasks = TaskBag()

    init(id: SampleItem.ID, repository: any SampleRepository) {
        self.id = id
        self.repository = repository
    }

    func send(_ event: Event) {
        switch event {
        case .appeared:
            if state.load == .idle {
                load()
            }
        case .retry:
            load()
        }
    }

    private func load() {
        tasks.latest(Self.loading) { [weak self] in
            await self?.performLoad()
        }
    }

    private func performLoad() async {
        state.load = .loading
        let result = await repository.item(id: id)
        guard !Task.isCancelled else { return }

        switch result {
        case .success(let item):
            state.item = item
            state.load = .loaded
        case .failure(let error):
            state.load = .failed(error)
        }
    }

    private static let loading = "load"
}
