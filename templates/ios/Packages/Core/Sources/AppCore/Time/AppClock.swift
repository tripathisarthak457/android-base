import Foundation

/// The current time, injected wherever code compares times, so tests can choose it.
public protocol AppClock: Sendable {
    var now: Date { get }
}

/// The device's clock.
public struct SystemClock: AppClock {
    public init() {}

    public var now: Date {
        .now
    }
}
