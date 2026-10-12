import AppCore
import Foundation
import Networking
import Testing

// Serialized: the stub's handler is one static property shared by every request.
@Suite(.serialized)
struct APIClientTests {
    @Test func decodesASuccessfulAnswer() async throws {
        let client = StubProtocol.client { _ in
            (200, #"{"id": 1, "title": "One"}"#)
        }
        let result = await client.send(Endpoint<Item>.get("items/1"))
        #expect(try result.get() == Item(id: 1, title: "One"))
    }

    @Test func resolvesPathsAgainstTheBaseURLAndKeepsAbsoluteOnes() async throws {
        let client = StubProtocol.client { request in
            (200, #"{"url": "\#(request.url?.absoluteString ?? "")"}"#)
        }
        let relative = await client.send(Endpoint<Echo>.get("items", query: [URLQueryItem(name: "page", value: "2")]))
        #expect(try relative.get().url == "https://api.example.com/v1/items?page=2")
        let absolute = await client.send(Endpoint<Echo>.get("https://other.example.com/feed"))
        #expect(try absolute.get().url == "https://other.example.com/feed")
    }

    @Test func turnsAnErrorStatusIntoAnHTTPFailure() async {
        let client = StubProtocol.client { _ in
            (503, #"{"code": "maintenance", "message": "Back soon", "errors": {"title": ["Too long"]}}"#)
        }
        let result = await client.send(Endpoint<Item>.get("items/1"))
        let expected = HTTPFailure(status: 503, code: "maintenance", message: "Back soon", fieldErrors: ["title": ["Too long"]])
        #expect(result == .failure(.http(expected)))
    }

    @Test func reportsABodyItCannotReadAsADecodeFailure() async {
        let client = StubProtocol.client { _ in
            (200, #"{"name": "no id"}"#)
        }
        let result = await client.send(Endpoint<Item>.get("items/1"))
        guard case .failure(.decode) = result else {
            Issue.record("Expected a decode failure, got \(result)")
            return
        }
    }

    @Test(arguments: [URLError.Code.notConnectedToInternet, .networkConnectionLost, .cannotFindHost])
    func reportsAMissingConnectionAsOffline(code: URLError.Code) async {
        let client = StubProtocol.client { _ in
            throw URLError(code)
        }
        #expect(await client.send(Endpoint<Item>.get("items/1")) == .failure(.offline))
    }

    @Test func reportsATimeoutAsATimeout() async {
        let client = StubProtocol.client { _ in
            throw URLError(.timedOut)
        }
        #expect(await client.send(Endpoint<Item>.get("items/1")) == .failure(.timeout))
    }
}

private struct Item: Decodable, Equatable, Sendable {
    let id: Int
    let title: String
}

private struct Echo: Decodable, Sendable {
    let url: String
}

/// Answers every request of the sessions it is installed in with what `handler` returns.
private final class StubProtocol: URLProtocol {
    typealias Handler = @Sendable (URLRequest) throws -> (status: Int, body: String)

    // Written by one test at a time; see the suite's `.serialized` trait.
    nonisolated(unsafe) static var handler: Handler?

    static func client(_ handler: @escaping Handler) -> APIClient {
        self.handler = handler
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubProtocol.self]
        let baseURL = URL(string: "https://api.example.com/v1/") ?? URL(filePath: "/")
        return APIClient(baseURL: baseURL, session: URLSession(configuration: configuration))
    }

    override class func canInit(with request: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard let handler = Self.handler, let url = request.url else { return }
        do {
            let answer = try handler(request)
            if let response = HTTPURLResponse(url: url, statusCode: answer.status, httpVersion: nil, headerFields: nil) {
                client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            }
            client?.urlProtocol(self, didLoad: Data(answer.body.utf8))
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}
}
