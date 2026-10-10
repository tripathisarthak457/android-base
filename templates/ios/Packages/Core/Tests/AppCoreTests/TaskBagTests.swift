import AppCore
import Testing

@MainActor
struct TaskBagTests {
    @Test func latestCancelsTheRunItReplaces() async {
        let bag = TaskBag()
        let recorder = Recorder()
        let first = bag.latest("load") {
            _ = try? await Task.sleep(for: .seconds(10))
            recorder.events.append(Task.isCancelled ? "first cancelled" : "first finished")
        }
        let second = bag.latest("load") {
            recorder.events.append("second finished")
        }
        await first.value
        await second.value
        #expect(recorder.events.sorted() == ["first cancelled", "second finished"])
        #expect(!bag.isRunning("load"))
    }

    @Test func debounceDropsARunReplacedBeforeItStarts() async {
        let bag = TaskBag()
        let recorder = Recorder()
        bag.latest("search", debounce: .milliseconds(50)) {
            recorder.events.append("a")
        }
        let last = bag.latest("search", debounce: .milliseconds(50)) {
            recorder.events.append("ab")
        }
        await last.value
        #expect(recorder.events == ["ab"])
    }

    @Test func exclusiveIgnoresASecondRunWhileTheFirstIsInFlight() {
        let bag = TaskBag()
        #expect(bag.exclusive("save") { _ = try? await Task.sleep(for: .seconds(10)) })
        #expect(!bag.exclusive("save") {})
        bag.cancel("save")
        #expect(!bag.isRunning("save"))
        #expect(bag.exclusive("save") {})
    }

    @Test func releasingTheBagCancelsItsWork() async {
        let recorder = Recorder()
        var bag: TaskBag? = TaskBag()
        let task = bag?.latest("load") {
            _ = try? await Task.sleep(for: .seconds(10))
            recorder.events.append(Task.isCancelled ? "cancelled" : "finished")
        }
        bag = nil
        await task?.value
        #expect(recorder.events == ["cancelled"])
    }
}

@MainActor
private final class Recorder {
    var events: [String] = []
}
