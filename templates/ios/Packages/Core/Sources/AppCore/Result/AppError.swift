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

    /// Nobody waits for the answer any more: a newer request replaced this one, or the screen that
    /// asked went away. Nothing failed, so nothing stands in for the answer and nothing is shown.
    case cancelled

    /// A bug: something failed that nothing above it was written to expect.
    case unexpected(String)
}
