import AppCore
import AppTesting
import SampleData
@testable import SampleFeature
import Testing

@MainActor
@Suite(.timeLimit(.minutes(1)))
struct SampleDetailStoreTests {
    @Test func loadsTheItemItWasOpenedFor() async {
        let store = SampleDetailStore(id: 2, repository: FakeSampleRepository(answer: .success(FakeSampleRepository.items)))
        store.send(.appeared)
        await waitUntil { store.state.load == .loaded }
        #expect(store.state.item == FakeSampleRepository.items[1])
    }

    @Test func aMissingItemIsAFailure() async {
        let store = SampleDetailStore(id: 99, repository: FakeSampleRepository(answer: .success(FakeSampleRepository.items)))
        store.send(.appeared)
        await waitUntil { store.state.load != .idle && store.state.load != .loading }
        #expect(store.state.load == .failed(.http(HTTPFailure(status: 404))))
    }
}
