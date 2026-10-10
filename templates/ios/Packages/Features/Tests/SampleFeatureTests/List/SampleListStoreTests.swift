import AppCore
import DesignSystem
import Networking
import SampleData
@testable import SampleFeature
import Testing

@MainActor
struct SampleListStoreTests {
    private let toasts = ToastCenter()

    @Test func appearingLoadsTheItems() async {
        let store = makeStore(FakeSampleRepository(answer: .success(FakeSampleRepository.items)))
        store.send(.appeared)
        await settle(store)
        #expect(store.state.load == .loaded)
        #expect(store.state.visibleItems == FakeSampleRepository.items)
    }

    @Test func anEmptyAnswerIsEmpty() async {
        let store = makeStore(FakeSampleRepository(answer: .success([])))
        store.send(.appeared)
        await settle(store)
        #expect(store.state.load == .empty)
    }

    @Test func aFailedFirstLoadShowsTheError() async {
        let store = makeStore(FakeSampleRepository(answer: .failure(.offline)))
        store.send(.appeared)
        await settle(store)
        #expect(store.state.load == .failed(.offline))
    }

    @Test func appearingAgainDoesNotReload() async {
        let repository = FakeSampleRepository(answer: .success(FakeSampleRepository.items))
        let store = makeStore(repository)
        store.send(.appeared)
        await settle(store)
        store.send(.appeared)
        await settle(store)
        #expect(await repository.requests == 1)
    }

    @Test func aFailedRefreshKeepsTheListAndSaysSo() async {
        let repository = FakeSampleRepository(answer: .success(FakeSampleRepository.items))
        let store = makeStore(repository)
        await store.refresh()
        await repository.answer(with: .failure(.timeout))

        await store.refresh()

        #expect(store.state.load == .loaded)
        #expect(store.state.items == FakeSampleRepository.items)
        #expect(toasts.current?.kind == .error)
    }

    @Test func searchingFiltersByTitle() async {
        let store = makeStore(FakeSampleRepository(answer: .success(FakeSampleRepository.items)))
        await store.refresh()
        store.send(.queryChanged("tw"))
        #expect(store.state.visibleItems.map(\.id) == [2])
        store.send(.queryChanged(""))
        #expect(store.state.visibleItems.count == 2)
    }

    private func makeStore(_ repository: FakeSampleRepository) -> SampleListStore {
        SampleListStore(repository: repository, monitor: NetworkMonitor(), toasts: toasts)
    }

    /// Waits for the store's load to finish. The fake answers at once, but the answer still crosses
    /// from its actor to the main actor, so this polls rather than counting on a number of turns.
    private func settle(_ store: SampleListStore) async {
        let deadline = ContinuousClock.now + .seconds(2)
        while store.state.load == .idle || store.state.load == .loading, ContinuousClock.now < deadline {
            try? await Task.sleep(for: .milliseconds(5))
        }
    }
}
