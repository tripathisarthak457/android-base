import Foundation

/// Why talking to the outside world failed.
///
/// Closed, so a `switch` over it is checked by the compiler and each screen decides what every case
/// means for the person using it. Underlying errors are kept as text: an `Error` is neither
/// `Equatable` nor something UI state should hold, and screens branch on the case, not the cause.
public enum AppError: Error, Equatable, Sendable {
    /// The device has no connection. Retrying makes sense once it has one.
    case offline

    /// The device is online, but no answer came back in time.
    case timeout

    /// The server answered with an error status.
    case http(HTTPFailure)

    /// The server answered, but not in the shape the app expects.
    case decode(String)

    /// The database or the settings store refused a read or a write.
    case storage(String)

    /// A bug: something failed that nothing above it was written to expect.
    case unexpected(String)
}

/// An error status from the server, and what the backend said about it.
public struct HTTPFailure: Equatable, Sendable {
    public let status: Int

    /// The backend's own error code, when its error body carries one.
    public let code: String?

    public let message: String?

    /// Messages per field, keyed by the field names the request used.
    public let fieldErrors: [String: [String]]

    /// How long the server asked the app to wait before trying again, from `Retry-After`.
    public let retryAfter: Duration?

    public init(
        status: Int,
        code: String? = nil,
        message: String? = nil,
        fieldErrors: [String: [String]] = [:],
        retryAfter: Duration? = nil
    ) {
        self.status = status
        self.code = code
        self.message = message
        self.fieldErrors = fieldErrors
        self.retryAfter = retryAfter
    }
}
