@testable import DesignSystem
import Testing

struct AppSheetGuardsTests {
    private let guards = AppSheetGuards()
    private let first = Owner()
    private let second = Owner()

    @Test func withoutAGuardTheCloseButtonClosesTheSheet() {
        #expect(!guards.attemptDismissal())
    }

    @Test func anEnabledGuardTakesTheAttempt() {
        var attempts = 0
        guards.setAttempt({ attempts += 1 }, for: first)

        #expect(guards.attemptDismissal())
        #expect(attempts == 1)
    }

    @Test func aDisabledGuardLetsTheSheetClose() {
        guards.setAttempt({}, for: first)
        guards.setAttempt(nil, for: first)

        #expect(!guards.attemptDismissal())
    }

    @Test func everyGuardHearsTheAttemptAsOnASwipe() {
        var heard: Set<String> = []
        guards.setAttempt({ heard.insert("first") }, for: first)
        guards.setAttempt({ heard.insert("second") }, for: second)

        #expect(guards.attemptDismissal())
        #expect(heard == ["first", "second"])
    }

    @Test func aGuardsLatestClosureIsTheOneCalled() {
        var called = ""
        guards.setAttempt({ called = "old" }, for: first)
        guards.setAttempt({ called = "new" }, for: first)

        _ = guards.attemptDismissal()

        #expect(called == "new")
    }
}

private final class Owner {}
