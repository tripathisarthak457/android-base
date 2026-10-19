import Foundation

/// The device's clock.
public struct SystemClock: AppClock {
    public init() {}

    public var now: Date {
        .now
    }
}
