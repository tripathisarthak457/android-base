import Navigation
import Testing

@MainActor
struct RouterTests {
    @Test func pushAndPopMoveOneScreenAtATime() {
        let router = Router()
        router.push(1)
        router.push("detail")
        #expect(router.depth == 2)
        router.pop()
        #expect(router.depth == 1)
    }

    @Test func popAtTheRootDoesNothing() {
        let router = Router()
        router.pop()
        #expect(router.depth == 0)
    }

    @Test func popToRootAlsoClearsTheSelection() {
        let router = Router()
        router.selection = "42"
        router.push(1)
        router.popToRoot()
        #expect(router.depth == 0)
        #expect(router.selection == nil)
    }
}
