@testable import DesignSystem
import Testing
import UIKit

struct DismissAttemptForwarderTests {
    private let presentation = UIPresentationController(presentedViewController: UIViewController(), presenting: nil)

    @Test func callsItDoesNotHandleReachTheReplacedDelegate() {
        let original = RecordingDelegate()
        let forwarder = DismissAttemptForwarder(forwardingTo: original)
        presentation.delegate = forwarder

        // Through the delegate property, as UIKit calls it: the runtime asks first, then forwards.
        presentation.delegate?.presentationControllerWillDismiss?(presentation)
        presentation.delegate?.presentationControllerDidDismiss?(presentation)

        #expect(original.calls == ["willDismiss", "didDismiss"])
    }

    @Test func itAnswersForExactlyWhatTheReplacedDelegateImplements() {
        let didDismiss = #selector(UIAdaptivePresentationControllerDelegate.presentationControllerDidDismiss(_:))
        let detentChange = #selector(
            UISheetPresentationControllerDelegate.sheetPresentationControllerDidChangeSelectedDetentIdentifier(_:)
        )
        let forwarder = DismissAttemptForwarder(forwardingTo: RecordingDelegate())

        #expect(forwarder.responds(to: didDismiss))
        #expect(!forwarder.responds(to: detentChange))
        #expect(!DismissAttemptForwarder(forwardingTo: nil).responds(to: didDismiss))
    }

    @Test func uninstallingGivesTheDelegateBack() {
        let original = RecordingDelegate()
        presentation.delegate = original
        let forwarder = DismissAttemptForwarder.install(on: presentation)
        #expect(presentation.delegate === forwarder)
        #expect(forwarder.isInstalled(on: presentation))

        forwarder.uninstall(from: presentation)

        #expect(presentation.delegate === original)
        #expect(!forwarder.isInstalled(on: presentation))
    }

    @Test func twoGuardsInOneSheetLeaveInEitherOrder() {
        let original = RecordingDelegate()
        presentation.delegate = original
        let first = DismissAttemptForwarder.install(on: presentation)
        let second = DismissAttemptForwarder.install(on: presentation)
        #expect(first.isInstalled(on: presentation))
        #expect(second.isInstalled(on: presentation))

        first.uninstall(from: presentation)

        #expect(presentation.delegate === second)
        #expect(second.original === original)
        presentation.delegate?.presentationControllerDidDismiss?(presentation)
        #expect(original.calls == ["didDismiss"])

        second.uninstall(from: presentation)
        #expect(presentation.delegate === original)
    }

    @Test func aDelegateSetAfterwardsIsNotTakenForTheForwarder() {
        let forwarder = DismissAttemptForwarder.install(on: presentation)
        let replacement = RecordingDelegate()
        presentation.delegate = replacement

        #expect(!forwarder.isInstalled(on: presentation))
        forwarder.uninstall(from: presentation)
        #expect(presentation.delegate === replacement)
    }

    @Test func whileGuardingEveryAttemptIsRefusedAndReported() {
        let original = RecordingDelegate()
        let forwarder = DismissAttemptForwarder(forwardingTo: original)
        var attempts = 0
        forwarder.onAttempt = { attempts += 1 }

        #expect(!forwarder.presentationControllerShouldDismiss(presentation))
        forwarder.presentationControllerDidAttemptToDismiss(presentation)
        forwarder.presentationControllerDidAttemptToDismiss(presentation)

        #expect(attempts == 2)
        #expect(original.calls == ["didAttemptToDismiss", "didAttemptToDismiss"])
    }

    @Test func otherwiseTheReplacedDelegateDecides() {
        let original = RecordingDelegate()
        let forwarder = DismissAttemptForwarder(forwardingTo: original)
        #expect(forwarder.presentationControllerShouldDismiss(presentation))

        original.allowsDismissal = false
        #expect(!forwarder.presentationControllerShouldDismiss(presentation))
        #expect(DismissAttemptForwarder(forwardingTo: nil).presentationControllerShouldDismiss(presentation))
    }
}

private final class RecordingDelegate: NSObject, UIAdaptivePresentationControllerDelegate {
    var calls: [String] = []
    var allowsDismissal = true

    func presentationControllerShouldDismiss(_ presentationController: UIPresentationController) -> Bool {
        allowsDismissal
    }

    func presentationControllerWillDismiss(_ presentationController: UIPresentationController) {
        calls.append("willDismiss")
    }

    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        calls.append("didDismiss")
    }

    func presentationControllerDidAttemptToDismiss(_ presentationController: UIPresentationController) {
        calls.append("didAttemptToDismiss")
    }
}
