import Foundation
import os

/// The person's settings, kept in `UserDefaults`, with a stream of changes the interface observes.
///
/// Reads and writes are synchronous and cheap, so it is safe to call from the main actor; every
/// write is sent to every `changes()` stream, so a toggle in Settings reaches the app's root
/// without the two knowing about each other.
public final class SettingsRepository: Sendable {
    // UserDefaults is thread-safe, as Apple documents, but not every SDK marks it Sendable.
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
            listeners.withLock { $0[id] = continuation }
            continuation.yield(current)
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

    private func broadcast() {
        let settings = current
        for listener in listeners.withLock({ Array($0.values) }) {
            listener.yield(settings)
        }
    }

    private enum Key {
        static let appearance = "settings.appearance"
        static let hapticsEnabled = "settings.hapticsEnabled"
        static let hasCompletedOnboarding = "settings.hasCompletedOnboarding"
    }
}
