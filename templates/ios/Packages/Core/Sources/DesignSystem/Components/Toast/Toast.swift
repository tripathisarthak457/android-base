import Foundation

/// A short message shown over the app for a moment: something finished, or failed in a way that
/// does not need a decision.
///
/// For an error the person has to act on, show an alert or the screen's own error state instead.
public nonisolated struct Toast: Identifiable, Equatable, Sendable {
    public enum Kind: Sendable {
        case info
        case success
        case error
    }

    public let id = UUID()
    /// What the toast says, already localised.
    public let message: String
    public let kind: Kind

    public init(_ message: String, kind: Kind = .info) {
        self.message = message
        self.kind = kind
    }
}
