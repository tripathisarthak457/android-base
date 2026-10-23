import Foundation

/// One request the app can make, and the type its answer decodes into.
///
/// Data modules declare their endpoints next to their DTOs:
///
///     static func orders(page: Int) -> Endpoint<[OrderDTO]> {
///         .get("orders", query: [URLQueryItem(name: "page", value: String(page))])
///     }
public struct Endpoint<Response: Decodable & Sendable>: Sendable {
    public var method: HTTPMethod
    /// Relative to the client's base URL, or an absolute `https` URL for another host.
    public var path: String
    public var query: [URLQueryItem]
    public var headers: [String: String]
    public var body: Data?
    /// `.reloadIgnoringLocalCacheData` makes a pull to refresh skip the HTTP cache.
    public var cachePolicy: URLRequest.CachePolicy

    public init(
        method: HTTPMethod,
        path: String,
        query: [URLQueryItem] = [],
        headers: [String: String] = [:],
        body: Data? = nil,
        cachePolicy: URLRequest.CachePolicy = .useProtocolCachePolicy
    ) {
        self.method = method
        self.path = path
        self.query = query
        self.headers = headers
        self.body = body
        self.cachePolicy = cachePolicy
    }

    public static func get(
        _ path: String,
        query: [URLQueryItem] = [],
        cachePolicy: URLRequest.CachePolicy = .useProtocolCachePolicy
    ) -> Endpoint {
        Endpoint(method: .get, path: path, query: query, cachePolicy: cachePolicy)
    }

    /// The request for this endpoint, or nil when `path` does not make a URL.
    func request(relativeTo baseURL: URL) -> URLRequest? {
        let resolved = path.hasPrefix("https://") || path.hasPrefix("http://")
            ? URL(string: path)
            : URL(string: path, relativeTo: baseURL)
        guard let resolved, var components = URLComponents(url: resolved, resolvingAgainstBaseURL: true) else {
            return nil
        }
        if !query.isEmpty {
            components.queryItems = (components.queryItems ?? []) + query
        }
        guard let url = components.url else { return nil }

        var request = URLRequest(url: url, cachePolicy: cachePolicy)
        request.httpMethod = method.rawValue
        request.httpBody = body
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if body != nil {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        for (field, value) in headers {
            request.setValue(value, forHTTPHeaderField: field)
        }
        return request
    }
}
