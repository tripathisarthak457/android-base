/// The only way a store starts work: named tasks with a policy for what happens when the same work
/// is asked for again.
///
/// - `latest(_:)` replaces: starting it again under a key cancels the run in flight (load, search).
/// - `exclusive(_:)` ignores: while a run under the key is in flight, a new one is dropped (submit,
///   save, pay), so a double tap cannot send twice.
///
/// Everything is cancelled when the bag is released with its store, so a screen that is gone stops
/// its work instead of finishing it for nobody.
@MainActor
public final class TaskBag {
    private var running: [String: Running] = [:]
    private var nextID = 0

    public init() {}

    /// Runs `operation` under `key`, cancelling whatever ran under it before.
    ///
    /// - Parameter debounce: how long to wait before starting. Another call within that time
    ///   replaces this one before it does anything, which is what a search field wants per keystroke.
    /// - Returns: the task, for a caller that has to wait for it, such as `refreshable`.
    @discardableResult
    public func latest(
        _ key: String,
        debounce: Duration = .zero,
        operation: @escaping @MainActor @Sendable () async -> Void
    ) -> Task<Void, Never> {
        start(key, debounce: debounce, operation: operation)
    }

    /// Runs `operation` under `key` unless a run under it is still in flight.
    ///
    /// - Returns: whether it started.
    @discardableResult
    public func exclusive(
        _ key: String,
        operation: @escaping @MainActor @Sendable () async -> Void
    ) -> Bool {
        guard running[key] == nil else { return false }
        start(key, debounce: .zero, operation: operation)
        return true
    }

    public func cancel(_ key: String) {
        running[key] = nil
    }

    public func cancelAll() {
        running.removeAll()
    }

    private func start(
        _ key: String,
        debounce: Duration,
        operation: @escaping @MainActor @Sendable () async -> Void
    ) -> Task<Void, Never> {
        nextID += 1
        let id = nextID
        let task = Task { [weak self] in
            if debounce > .zero {
                do {
                    try await Task.sleep(for: debounce)
                } catch {
                    return
                }
            }
            await operation()
            self?.finished(key, id: id)
        }
        running[key] = Running(id: id, task: task)
        return task
    }

    private func finished(_ key: String, id: Int) {
        // A run that was replaced must not remove the run that replaced it.
        if running[key]?.id == id {
            running[key] = nil
        }
    }
}

/// A task that is cancelled when nothing holds it any more, which is what makes replacing, removing
/// or releasing an entry of the bag cancel its work.
private final class Running: Sendable {
    let id: Int
    let task: Task<Void, Never>

    init(id: Int, task: Task<Void, Never>) {
        self.id = id
        self.task = task
    }

    deinit {
        task.cancel()
    }
}
