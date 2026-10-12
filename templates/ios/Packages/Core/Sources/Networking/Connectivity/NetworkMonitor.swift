import Foundation
import Network
import os

/// Whether the device has a usable network path, from one `NWPathMonitor` shared by the whole app.
///
/// It is a hint for the interface and a trigger to retry, never a gate: `APIClient` attempts every
/// request whatever this says, because a path can be reported unsatisfied while a request would
/// still succeed, and the other way round.
public final class NetworkMonitor: Sendable {
    private let monitor = NWPathMonitor()
    private let state = OSAllocatedUnfairLock(initialState: State())

    private struct State: Sendable {
        var isOnline = true
        var listeners: [UUID: AsyncStream<Bool>.Continuation] = [:]
    }

    public init() {
        monitor.pathUpdateHandler = { [weak self] path in
            self?.update(isOnline: path.status == .satisfied)
        }
        monitor.start(queue: DispatchQueue(label: "NetworkMonitor", qos: .utility))
    }

    deinit {
        monitor.cancel()
    }

    /// The latest answer. Before the first path arrives it is `true`, so nothing starts out
    /// claiming the device is offline.
    public var isOnline: Bool {
        state.withLock { $0.isOnline }
    }

    /// The current answer, then every change, until the consuming task ends.
    public func updates() -> AsyncStream<Bool> {
        AsyncStream { continuation in
            let id = UUID()
            // Yielded under the lock, as changes are, so a change that lands meanwhile reaches the
            // stream after this snapshot rather than before it, and the last value is the latest.
            state.withLock { state in
                state.listeners[id] = continuation
                continuation.yield(state.isOnline)
            }
            continuation.onTermination = { [weak self] _ in
                self?.state.withLock { _ = $0.listeners.removeValue(forKey: id) }
            }
        }
    }

    private func update(isOnline: Bool) {
        state.withLock { state in
            guard state.isOnline != isOnline else { return }
            state.isOnline = isOnline
            for listener in state.listeners.values {
                listener.yield(isOnline)
            }
        }
    }
}
