import Foundation

/// The current time, injected wherever code compares times, so tests can choose it.
public protocol AppClock: Sendable {
    var now: Date { get }
}
