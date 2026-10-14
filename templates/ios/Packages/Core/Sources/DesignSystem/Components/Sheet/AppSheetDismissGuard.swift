import SwiftUI
import UIKit

extension View {
    /// Keeps the sheet this view is in from closing by a swipe down or a tap outside while
    /// `enabled`, and calls `onAttempt` each time someone tries, so the sheet can ask before
    /// discarding their changes.
    ///
    /// The sheet stretches and springs back, as one that is not `dismissible` does. The close button
    /// of `appSheetHeader` calls `onAttempt` too; buttons of the sheet's own still close it, so a
    /// Cancel button asks the same question first. Apply it inside the sheet's content, on the view
    /// that owns the changes.
    public func appSheetDismissGuard(enabled: Bool, onAttempt: @escaping () -> Void) -> some View {
        modifier(AppSheetDismissGuard(isEnabled: enabled, holdsInteractiveDismissal: true, onAttempt: onAttempt))
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

struct AppSheetDismissGuard: ViewModifier {
    let isEnabled: Bool

    /// Whether a swipe down or a tap outside is held and reported too. Without it only the close
    /// button of `appSheetHeader` asks, for a sheet whose swipes the system confirms by itself.
    let holdsInteractiveDismissal: Bool

    let onAttempt: () -> Void

    @Environment(\.isAppSheetDismissible) private var isDismissible
    @Environment(\.appSheetGuards) private var guards

    func body(content: Content) -> some View {
        content
            // SwiftUI documents no order between nested `interactiveDismissDisabled` modifiers, so
            // this one also carries the sheet's own setting rather than overriding it.
            .interactiveDismissDisabled((holdsInteractiveDismissal && isEnabled) || !isDismissible)
            .background {
                DismissAttemptObserver(
                    onAttempt: isEnabled ? onAttempt : nil,
                    guards: guards,
                    holdsInteractiveDismissal: holdsInteractiveDismissal
                )
                .allowsHitTesting(false)
                .accessibilityHidden(true)
            }
    }
}

/// UIKit reports a refused dismissal only to the presentation controller's delegate, which is
/// SwiftUI's own. The observer puts a `DismissAttemptForwarder` in front of it for as long as the
/// guarded view is in the sheet, and registers the guard with the sheet's close button.
private struct DismissAttemptObserver: UIViewControllerRepresentable {
    let onAttempt: (() -> Void)?
    let guards: AppSheetGuards?
    let holdsInteractiveDismissal: Bool

    func makeUIViewController(context: Context) -> DismissAttemptController {
        DismissAttemptController()
    }

    func updateUIViewController(_ controller: DismissAttemptController, context: Context) {
        controller.update(onAttempt: onAttempt, guards: guards, holdsInteractiveDismissal: holdsInteractiveDismissal)
    }

    static func dismantleUIViewController(_ controller: DismissAttemptController, coordinator: Coordinator) {
        controller.update(onAttempt: nil, guards: nil, holdsInteractiveDismissal: false)
    }
}

private final class DismissAttemptController: UIViewController {
    private var onAttempt: (() -> Void)?
    private var holdsInteractiveDismissal = false
    private var forwarder: DismissAttemptForwarder?
    private weak var presentation: UIPresentationController?
    private weak var guards: AppSheetGuards?

    func update(onAttempt: (() -> Void)?, guards: AppSheetGuards?, holdsInteractiveDismissal: Bool) {
        self.onAttempt = onAttempt
        self.holdsInteractiveDismissal = holdsInteractiveDismissal
        if guards !== self.guards {
            self.guards?.setAttempt(nil, for: self)
            self.guards = guards
        }
        guards?.setAttempt(onAttempt, for: self)
        if holdsInteractiveDismissal {
            forwarder?.onAttempt = onAttempt
            startForwarding()
        } else {
            stopForwarding()
        }
    }

    override func loadView() {
        view = UIView()
        view.isUserInteractionEnabled = false
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if holdsInteractiveDismissal {
            startForwarding()
        }
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

    private func stopForwarding() {
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
