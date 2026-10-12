import AppTesting
import Observation
import Testing

@MainActor
@Suite(.timeLimit(.minutes(1)))
struct WaitUntilTests {
    @Test func returnsAtOnceWhenTheConditionAlreadyHolds() async {
        let counter = Counter()
        await waitUntil { counter.value == 0 }
    }

    @Test func returnsOnceAChangeMakesTheConditionHold() async {
        let counter = Counter()
        Task {
            counter.value = 1
            counter.value = 2
        }
        await waitUntil { counter.value == 2 }
        #expect(counter.value == 2)
    }
}

@MainActor
@Observable
private final class Counter {
    var value = 0
}
