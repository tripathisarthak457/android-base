import SwiftUI
import UIKit

extension View {
    /// Keeps the sheet this view is in from closing by a swipe down or a tap outside while
    /// `enabled`, and calls `onAttempt` each time someone tries, so the sheet can ask before
    /// discarding their changes.
    ///
    /// The sheet stretches and springs back, as one that is not `dismissible` does. Its own buttons
    /// still close it, so its Cancel button asks the same question first. Apply it inside the
    /// sheet's content, on the view that owns the changes.
    public func appSheetDismissGuard(enabled: Bool, onAttempt: @escaping () -> Void) -> some View {
        modifier(AppSheetDismissGuard(isEnabled: enabled, onAttempt: onAttempt))
    }
}

extension EnvironmentValues {
    /// The `dismissible` option of the sheet around this view.
    nonisolated var isAppSheetDismissible: Bool {
        get { self[AppSheetDismissibleKey.self] }
        set { self[AppSheetDismissibleKey.self] = newValue }
    }
}

private nonisolated struct AppSheetDismissibleKey: EnvironmentKey {
    static let defaultValue = true
}

private struct AppSheetDismissGuard: ViewModifier {
    let isEnabled: Bool
    let onAttempt: () -> Void

    @Environment(\.isAppSheetDismissible) private var isDismissible

    func body(content: Content) -> some View {
        content
            // SwiftUI documents no order between nested `interactiveDismissDisabled` modifiers, so
            // this one also carries the sheet's own setting rather than overriding it.
            .interactiveDismissDisabled(isEnabled || !isDismissible)
            .background {
                DismissAttemptObserver(onAttempt: isEnabled ? onAttempt : nil)
                    .allowsHitTesting(false)
                    .accessibilityHidden(true)
            }
    }
}

/// UIKit reports a refused dismissal only to the presentation controller's delegate, which is
/// SwiftUI's own. The observer puts a `DismissAttemptForwarder` in front of it for as long as the
/// guarded view is in the sheet.
private struct DismissAttemptObserver: UIViewControllerRepresentable {
    let onAttempt: (() -> Void)?

    func makeUIViewController(context: Context) -> DismissAttemptController {
        DismissAttemptController()
    }

    func updateUIViewController(_ controller: DismissAttemptController, context: Context) {
        controller.onAttempt = onAttempt
    }

    static func dismantleUIViewController(_ controller: DismissAttemptController, coordinator: Coordinator) {
        controller.stopForwarding()
    }
}

private final class DismissAttemptController: UIViewController {
    var onAttempt: (() -> Void)? {
        didSet {
            forwarder?.onAttempt = onAttempt
            startForwarding()
        }
    }

    private var forwarder: DismissAttemptForwarder?
    private weak var presentation: UIPresentationController?

    override func loadView() {
        view = UIView()
        view.isUserInteractionEnabled = false
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        startForwarding()
    }

    /// Puts a forwarder in front of the delegate of the presentation this view is in, unless this
    /// view's forwarder is already there. Called again on every update, so a delegate SwiftUI sets
    /// afresh is wrapped too.
    private func startForwarding() {
        guard let presentation = enclosingPresentation() else { return }
        if let forwarder, presentation === self.presentation, forwarder.isInstalled(on: presentation) {
            return
        }
        stopForwarding()
        let forwarder = DismissAttemptForwarder.install(on: presentation)
        forwarder.onAttempt = onAttempt
        self.forwarder = forwarder
        self.presentation = presentation
    }

    func stopForwarding() {
        if let forwarder, let presentation {
            forwarder.uninstall(from: presentation)
        }
        forwarder = nil
        presentation = nil
    }

    /// The presentation of the sheet this view is in, or nil outside one. Read from the presented
    /// controller itself: asking a child for its `presentationController` can create one.
    private func enclosingPresentation() -> UIPresentationController? {
        var root: UIViewController = self
        while let parent = root.parent {
            root = parent
        }
        return root.presentingViewController == nil ? nil : root.presentationController
    }
}

#Preview("Dismiss guard") {
    @Previewable @State var isPresented = true
    @Previewable @State var note = ""
    @Previewable @State var asksToDiscard = false
    Button {
        isPresented = true
    } label: {
        Text(verbatim: "Show the sheet")
    }
    .appSheet(isPresented: $isPresented) {
        TextField(text: $note) {
            Text(verbatim: "Type, then swipe the sheet down")
        }
        .padding(AppSpacing.gutter)
        .appSheetDismissGuard(enabled: !note.isEmpty) {
            asksToDiscard = true
        }
        .confirmationDialog(Text(verbatim: "Discard the note?"), isPresented: $asksToDiscard) {
            Button(role: .destructive) {
                isPresented = false
            } label: {
                Text(verbatim: "Discard")
            }
        }
    }
    .appTheme()
}
