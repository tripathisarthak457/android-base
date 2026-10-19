import DesignSystem
import Testing

@MainActor
struct ToastCenterTests {
    @Test func toastsWaitForTheOneBeforeThem() {
        let center = ToastCenter()
        let first = Toast("Saved", kind: .success)
        let second = Toast("Could not sync", kind: .error)
        center.show(first)
        center.show(second)
        #expect(center.current == first)

        center.dismiss(first)
        #expect(center.current == second)

        center.dismiss(second)
        #expect(center.current == nil)
    }

    @Test func dismissingAToastThatIsNotShownChangesNothing() {
        let center = ToastCenter()
        let shown = Toast("Saved")
        center.show(shown)
        center.show(Toast("Queued"))
        center.dismiss(Toast("Never shown"))
        #expect(center.current == shown)
    }
}
