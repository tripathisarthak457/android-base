import UIKit

/// Stands in for a presentation controller's delegate. While `onAttempt` is set it refuses every
/// dismissal people start and reports each one; every call, those included, also reaches the
/// delegate it replaced.
///
/// The replaced delegate is SwiftUI's: it resets the sheet's binding when the sheet is swiped away
/// and follows its detent, so nothing may be lost on the way to it. Two guarded views in one sheet
/// put two forwarders in front of it, one behind the other.
final class DismissAttemptForwarder: NSObject, UISheetPresentationControllerDelegate {
    // Read by the runtime's forwarding hooks, which are nonisolated; written only on the main actor.
    nonisolated(unsafe) weak var original: (any UIAdaptivePresentationControllerDelegate)?

    var onAttempt: (() -> Void)?

    init(forwardingTo original: (any UIAdaptivePresentationControllerDelegate)?) {
        self.original = original
        super.init()
    }

    /// Puts a new forwarder in front of `presentation`'s delegate.
    static func install(on presentation: UIPresentationController) -> DismissAttemptForwarder {
        let forwarder = DismissAttemptForwarder(forwardingTo: presentation.delegate)
        presentation.delegate = forwarder
        return forwarder
    }

    /// Whether this forwarder is `presentation`'s delegate or behind another forwarder that is.
    func isInstalled(on presentation: UIPresentationController) -> Bool {
        var link = presentation.delegate as? DismissAttemptForwarder
        while let current = link {
            if current === self {
                return true
            }
            link = current.original as? DismissAttemptForwarder
        }
        return false
    }

    /// Hands this forwarder's place to the delegate it replaced, wherever it is in the chain. The
    /// delegate property and `original` are weak, so a forwarder that went without this would cut
    /// SwiftUI off from the presentation.
    func uninstall(from presentation: UIPresentationController) {
        if presentation.delegate === self {
            presentation.delegate = original
            return
        }
        var link = presentation.delegate as? DismissAttemptForwarder
        while let current = link {
            if current.original === self {
                current.original = original
                return
            }
            link = current.original as? DismissAttemptForwarder
        }
    }

    func presentationControllerShouldDismiss(_ presentationController: UIPresentationController) -> Bool {
        // UIKit asks only about a dismissal `isModalInPresentation` has not already refused.
        // Refusing here too keeps the guard working whatever the sheet's nested
        // `interactiveDismissDisabled` modifiers resolved to.
        if onAttempt != nil {
            return false
        }
        return original?.presentationControllerShouldDismiss?(presentationController) ?? true
    }

    func presentationControllerDidAttemptToDismiss(_ presentationController: UIPresentationController) {
        original?.presentationControllerDidAttemptToDismiss?(presentationController)
        onAttempt?()
    }

    nonisolated override func responds(to selector: Selector!) -> Bool {
        if super.responds(to: selector) {
            return true
        }
        return original?.responds(to: selector) ?? false
    }

    nonisolated override func forwardingTarget(for selector: Selector!) -> Any? {
        if let original, original.responds(to: selector) {
            return original
        }
        return super.forwardingTarget(for: selector)
    }
}
