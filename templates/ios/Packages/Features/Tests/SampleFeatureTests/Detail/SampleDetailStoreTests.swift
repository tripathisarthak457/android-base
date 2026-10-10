import AppCore
import SampleData
@testable import SampleFeature
import Testing

@MainActor
struct SampleDetailStoreTests {
    @Test func loadsTheItemItWasOpenedFor() async {
        let store = SampleDetailStore(id: 2, repository: FakeSampleRepository(answer: .success(FakeSampleRepository.items)))
        store.send(.appeared)
        await settle(store)
        #expect(store.state.item == FakeSampleRepository.items[1])
        #expect(store.state.load == .loaded)
    }

    @Test func aMissingItemIsAFailure() async {
        let store = SampleDetailStore(id: 99, repository: FakeSampleRepository(answer: .success(FakeSampleRepository.items)))
        store.send(.appeared)
        await settle(store)
        #expect(store.state.load == .failed(.http(HTTPFailure(status: 404))))
    }

    /// Waits for the store's load to finish. The fake answers at once, but the answer still crosses
    /// from its actor to the main actor, so this polls rather than counting on a number of turns.
    private func settle(_ store: SampleDetailStore) async {
        let deadline = ContinuousClock.now + .seconds(2)
        while store.state.load == .idle || store.state.load == .loading, ContinuousClock.now < deadline {
            try? await Task.sleep(for: .milliseconds(5))
        }
    }
}
