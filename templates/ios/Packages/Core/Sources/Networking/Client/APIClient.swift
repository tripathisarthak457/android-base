import AppCore
import Foundation

/// The app's one way to reach its backend: async/await over an injected `URLSession`, with every
/// failure turned into an `AppError`.
///
/// It never checks connectivity first. A request is always attempted, and a failure that means the
/// device is offline comes back as `.offline`, which is both faster and more truthful than asking
/// the network monitor.
public struct APIClient: Sendable {
    private let baseURL: URL
    private let session: URLSession

    /// - Parameter session: tests pass a session whose configuration routes requests to a stub.
    public init(baseURL: URL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session
    }

    public func send<Response>(_ endpoint: Endpoint<Response>) async -> AppResult<Response> {
        guard let request = endpoint.request(relativeTo: baseURL) else {
            return .failure(.unexpected("Endpoint path '\(endpoint.path)' does not make a URL."))
        }

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch let error as URLError {
            return .failure(Self.failure(for: error))
        } catch is CancellationError {
            return .failure(.cancelled)
        } catch {
            return .failure(.unexpected(String(describing: error)))
        }

        guard let http = response as? HTTPURLResponse else {
            return .failure(.unexpected("The response to \(request.url?.absoluteString ?? endpoint.path) was not HTTP."))
        }
        guard (200..<300).contains(http.statusCode) else {
            return .failure(.http(await Self.httpFailure(status: http.statusCode, body: data)))
        }
        return await Self.decode(Response.self, from: data)
    }

    // `@concurrent` runs these on the shared pool whatever actor called `send`, so a large payload
    // is parsed while the main actor keeps drawing frames.
    @concurrent
    private static func decode<Value: Decodable & Sendable>(_ type: Value.Type, from data: Data) async -> AppResult<Value> {
        do {
            return .success(try JSONDecoder().decode(type, from: data))
        } catch {
            return .failure(.decode(String(describing: error)))
        }
    }

    @concurrent
    private static func httpFailure(status: Int, body: Data) async -> HTTPFailure {
        let details = try? JSONDecoder().decode(ErrorBody.self, from: body)
        return HTTPFailure(
            status: status,
            code: details?.code,
            message: details?.message,
            fieldErrors: details?.errors ?? [:]
        )
    }

    private static func failure(for error: URLError) -> AppError {
        switch error.code {
        case .timedOut:
            .timeout
        case .cancelled:
            .cancelled
        case .notConnectedToInternet, .networkConnectionLost, .dataNotAllowed, .internationalRoamingOff,
             .cannotFindHost, .cannotConnectToHost, .dnsLookupFailed:
            .offline
        default:
            .unexpected(error.localizedDescription)
        }
    }
}

/// The error body the backend sends with a failed request. Every field is optional, so a backend
/// with a different shape still yields the status.
private struct ErrorBody: Decodable, Sendable {
    let code: String?
    let message: String?
    let errors: [String: [String]]?
}
