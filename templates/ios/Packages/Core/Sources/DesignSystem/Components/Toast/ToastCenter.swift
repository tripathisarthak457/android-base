import Accessibility
import AppCore
import Observation

/// The queue of toasts for one window. Stores receive it at init and call `show(_:)`; the window's
/// root draws it with `toastHost(_:)`.
///
/// One toast shows at a time and each waits for the one before it, so two quick failures are both
/// read rather than one replacing the other.
@MainActor
@Observable
public final class ToastCenter {
    /// The toast on screen, if any.
    public private(set) var current: Toast?

    private var queue: [Toast] = []
    private let tasks = TaskBag()

    public init() {}

    public func show(_ toast: Toast) {
        queue.append(toast)
        if current == nil {
            advance()
        }
    }

    /// Dismisses `toast` if it is the one on screen, and shows the next.
    public func dismiss(_ toast: Toast) {
        guard current?.id == toast.id else { return }
        advance()
    }

    private func advance() {
        current = queue.isEmpty ? nil : queue.removeFirst()
        guard let shown = current else {
            tasks.cancel(Self.dismissal)
            return
        }
        AccessibilityNotification.Announcement(shown.message).post()
        tasks.latest(Self.dismissal, debounce: Self.duration(of: shown.kind)) { [weak self] in
            self?.dismiss(shown)
        }
    }

    private static let dismissal = "dismissal"

    /// Long enough to read the message twice; errors stay longer because they are read more carefully.
    private static func duration(of kind: Toast.Kind) -> Duration {
        switch kind {
        case .info, .success: .seconds(3)
        case .error: .seconds(5)
        }
    }
}
