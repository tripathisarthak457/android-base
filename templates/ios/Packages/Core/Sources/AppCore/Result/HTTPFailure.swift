/// An error status from the server, and what the backend said about it.
public struct HTTPFailure: Equatable, Sendable {
    public let status: Int

    /// The backend's own error code, when its error body carries one.
    public let code: String?

    public let message: String?

    /// Messages per field, keyed by the field names the request used.
    public let fieldErrors: [String: [String]]

    public init(status: Int, code: String? = nil, message: String? = nil, fieldErrors: [String: [String]] = [:]) {
        self.status = status
        self.code = code
        self.message = message
        self.fieldErrors = fieldErrors
    }
}
