import AppCore
import AppTesting
import DesignSystem
import os
import SampleData
@testable import SampleFeature
import Testing

@MainActor
@Suite(.timeLimit(.minutes(1)))
struct SampleListStoreTests {
    private let toasts = ToastCenter()
    private let network = AsyncStream.makeStream(of: Bool.self)

    @Test func appearingLoadsTheItems() async {
        let store = makeStore(FakeSampleRepository(answer: .success(FakeSampleRepository.items)))
        store.send(.appeared)
        await waitUntil { store.state.load == .loaded }
        #expect(store.state.visibleItems == FakeSampleRepository.items)
    }

    @Test func anEmptyAnswerIsEmpty() async {
        let store = makeStore(FakeSampleRepository(answer: .success([])))
        store.send(.appeared)
        await waitUntil { store.state.load == .empty }
        #expect(store.state.items.isEmpty)
    }

    @Test func aFailedFirstLoadShowsTheError() async {
        let store = makeStore(FakeSampleRepository(answer: .failure(.offline)))
        store.send(.appeared)
        await waitUntil { store.state.load == .failed(.offline) }
        #expect(store.state.items.isEmpty)
    }

    @Test func appearingAgainDoesNotReload() async {
        let repository = FakeSampleRepository(answer: .success(FakeSampleRepository.items))
        let store = makeStore(repository)
        store.send(.appeared)
        await waitUntil { store.state.load == .loaded }
        store.send(.appeared)
        await store.refresh()
        // One load for the first appearance, one for the refresh, none for the second appearance.
        #expect(await repository.requests == 2)
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

    @Test func followsTheConnection() async {
        let store = makeStore(FakeSampleRepository(answer: .success(FakeSampleRepository.items)))
        store.send(.appeared)

        network.continuation.yield(false)
        await waitUntil { !store.state.isOnline }

        network.continuation.yield(true)
        await waitUntil { store.state.isOnline }
    }

    @Test func aLoadThatFailedOfflineRunsAgainWhenTheConnectionReturns() async {
        let repository = FakeSampleRepository(answer: .failure(.offline))
        let store = makeStore(repository)
        store.send(.appeared)
        await waitUntil { store.state.load == .failed(.offline) }
        network.continuation.yield(false)
        await waitUntil { !store.state.isOnline }
        await repository.answer(with: .success(FakeSampleRepository.items))

        network.continuation.yield(true)

        await waitUntil { store.state.load == .loaded }
        #expect(store.state.items == FakeSampleRepository.items)
    }

    @Test func aLoadThatFailedForAnotherReasonWaitsForARetry() async {
        let repository = FakeSampleRepository(answer: .failure(.timeout))
        let store = makeStore(repository)
        store.send(.appeared)
        await waitUntil { store.state.load == .failed(.timeout) }

        network.continuation.yield(false)
        network.continuation.yield(true)
        await waitUntil { store.state.isOnline }

        #expect(store.state.load == .failed(.timeout))
        #expect(await repository.requests == 1)
    }

    @Test func appearingAgainKeepsOneSubscription() async {
        let subscriptions = OSAllocatedUnfairLock(initialState: 0)
        let store = SampleListStore(
            repository: FakeSampleRepository(answer: .success(FakeSampleRepository.items)),
            connectivity: {
                subscriptions.withLock { $0 += 1 }
                return AsyncStream { $0.yield(false) }
            },
            toasts: toasts
        )
        store.send(.appeared)
        await waitUntil { !store.state.isOnline && store.state.load == .loaded }

        store.send(.appeared)
        // The main actor runs its work in order, so a second subscription started by the line above
        // would have asked for its stream before this refresh is done.
        await store.refresh()

        #expect(subscriptions.withLock { $0 } == 1)
    }

    private func makeStore(_ repository: FakeSampleRepository) -> SampleListStore {
        SampleListStore(repository: repository, connectivity: { [stream = network.stream] in stream }, toasts: toasts)
    }
}
