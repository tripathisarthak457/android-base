import AppCore
import Testing

struct LoadStateTests {
    @Test(arguments: [LoadState.loaded, .refreshing])
    func contentStaysOnScreen(while state: LoadState) {
        #expect(state.hasContent)
    }

    @Test(arguments: [LoadState.idle, .loading, .empty, .failed(.offline)])
    func thereIsNoContent(while state: LoadState) {
        #expect(!state.hasContent)
    }
}
