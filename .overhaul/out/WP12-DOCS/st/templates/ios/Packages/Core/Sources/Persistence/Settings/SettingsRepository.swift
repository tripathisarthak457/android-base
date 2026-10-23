import Foundation
import os

/// The person's settings, kept in `UserDefaults`, with a stream of changes the interface observes.
///
/// Reads and writes are synchronous and cheap, so it is safe to call from the main actor; every
/// write is sent to every `changes()` stream, so a toggle in Settings reaches the app's root
/// without the two knowing about each other.
public final class SettingsRepository: Sendable {
    // UserDefaults is thread-safe, as Apple documents, but the SDK does not mark it Sendable.
    private nonisolated(unsafe) let defaults: UserDefaults
    private let hapticsDefault: Bool
    private let listeners = OSAllocatedUnfairLock(initialState: [UUID: AsyncStream<AppSettings>.Continuation]())

    /// - Parameters:
    ///   - defaults: tests pass a suite of their own, so they never touch the app's.
    ///   - hapticsDefault: the value before the person has chosen, which the project was generated with.
    public init(defaults: UserDefaults = .standard, hapticsDefault: Bool) {
        self.defaults = defaults
        self.hapticsDefault = hapticsDefault
    }

    public var current: AppSettings {
        AppSettings(
            appearance: defaults.string(forKey: Key.appearance).flatMap(Appearance.init(rawValue:)) ?? .system,
            hapticsEnabled: defaults.object(forKey: Key.hapticsEnabled) as? Bool ?? hapticsDefault,
            hasCompletedOnboarding: defaults.bool(forKey: Key.hasCompletedOnboarding)
        )
    }

    /// The current settings, then the settings after every write, until the consuming task ends.
    public func changes() -> AsyncStream<AppSettings> {
        AsyncStream { continuation in
            let id = UUID()
            listeners.withLock { listeners in
                listeners[id] = continuation
                continuation.yield(current)
            }
            continuation.onTermination = { [weak self] _ in
                self?.listeners.withLock { _ = $0.removeValue(forKey: id) }
            }
        }
    }

    public func setAppearance(_ appearance: Appearance) {
        defaults.set(appearance.rawValue, forKey: Key.appearance)
        broadcast()
    }

    public func setHapticsEnabled(_ enabled: Bool) {
        defaults.set(enabled, forKey: Key.hapticsEnabled)
        broadcast()
    }

    public func setOnboardingCompleted() {
        defaults.set(true, forKey: Key.hasCompletedOnboarding)
        broadcast()
    }

    // Settings are read and yielded under the lock, here and for a new stream's first value, so
    // when two writes race, the broadcast that runs last reads both, and no stream ends on a value
    // older than what `defaults` holds.
    private func broadcast() {
        listeners.withLock { listeners in
            let settings = current
            for listener in listeners.values {
                listener.yield(settings)
            }
        }
    }

    private enum Key {
        static let appearance = "settings.appearance"
        static let hapticsEnabled = "settings.hapticsEnabled"
        static let hasCompletedOnboarding = "settings.hasCompletedOnboarding"
    }
}
