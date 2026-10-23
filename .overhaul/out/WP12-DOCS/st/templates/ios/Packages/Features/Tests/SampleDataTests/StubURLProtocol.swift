import Foundation
// <opt:database>
import os
// </opt:database>

/// Answers every request of the sessions it is installed in with what `handler` returns.
final class StubURLProtocol: URLProtocol {
    enum Answer: Sendable {
        case answer(String, status: Int = 200)
        case failure(URLError)
    }

    typealias Handler = @Sendable (URLRequest) -> Answer

    // Written by one test at a time; the suites that use it are `.serialized`.
    nonisolated(unsafe) static var handler: Handler?

    override class func canInit(with request: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard let handler = Self.handler, let url = request.url else { return }
        switch handler(request) {
        case .answer(let body, let status):
            if let response = HTTPURLResponse(url: url, statusCode: status, httpVersion: nil, headerFields: nil) {
                client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            }
            client?.urlProtocol(self, didLoad: Data(body.utf8))
            client?.urlProtocolDidFinishLoading(self)
        case .failure(let error):
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}
}
// <opt:database>

/// A count a stub's handler can bump from URLSession's own queue.
final class RequestCounter: Sendable {
    private let count = OSAllocatedUnfairLock(initialState: 0)

    var value: Int {
        count.withLock { $0 }
    }

    func increment() {
        count.withLock { $0 += 1 }
    }
}
// </opt:database>
