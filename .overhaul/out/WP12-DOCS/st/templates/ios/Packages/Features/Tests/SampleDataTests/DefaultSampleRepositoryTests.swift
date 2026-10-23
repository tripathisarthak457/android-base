import AppCore
import Foundation
import Networking
@testable import SampleData
import Testing
// <opt:database>
import Persistence
import SwiftData
// </opt:database>

// Serialized: the stub's handler is one static property shared by every request.
@Suite(.serialized)
struct DefaultSampleRepositoryTests {
    @Test func mapsTheServersList() async throws {
        let repository = try makeRepository { _ in
            .answer(#"[{"id": 1, "title": " One ", "body": "First"}]"#)
        }
        #expect(try await repository.items(forceRefresh: false).get() == [SampleItem(id: 1, title: "One", body: "First")])
    }

    @Test func passesTheServersFailureOn() async throws {
        let repository = try makeRepository { _ in .failure(URLError(.notConnectedToInternet)) }
        #expect(await repository.items(forceRefresh: false) == .failure(.offline))
    }
    // <opt:database>

    @Test func aFreshSavedListNeedsNoRequest() async throws {
        let counter = RequestCounter()
        let repository = try makeRepository { _ in
            counter.increment()
            return .answer(#"[{"id": 1, "title": "One", "body": "First"}]"#)
        }
        _ = await repository.items(forceRefresh: false)
        _ = await repository.items(forceRefresh: false)
        #expect(counter.value == 1)
    }

    @Test func theSavedListStandsInWhenTheServerCannotBeReached() async throws {
        let later = try await repositoryAfterASave(then: .failure(URLError(.notConnectedToInternet)))
        #expect(try await later.items(forceRefresh: false).get() == [SampleItem(id: 1, title: "One", body: "First")])
    }

    @Test func aFailedRefreshReportsItsFailure() async throws {
        let later = try await repositoryAfterASave(then: .answer("", status: 500))
        #expect(await later.items(forceRefresh: true) == .failure(.http(HTTPFailure(status: 500))))
    }

    @Test func aCancelledRequestGetsNoStandIn() async throws {
        let later = try await repositoryAfterASave(then: .failure(URLError(.cancelled)))
        #expect(await later.items(forceRefresh: false) == .failure(.cancelled))
    }

    /// A repository an hour after another one saved a list, whose server now answers `answer`.
    private func repositoryAfterASave(then answer: StubURLProtocol.Answer) async throws -> DefaultSampleRepository {
        let requests = RequestCounter()
        let handler: StubURLProtocol.Handler = { _ in
            requests.increment()
            return requests.value == 1 ? .answer(#"[{"id": 1, "title": "One", "body": "First"}]"#) : answer
        }
        let container = try ModelContainerFactory.make(for: SampleStorage.models, inMemory: true)
        let now = Date.now
        _ = try await makeRepository(container: container, clock: FixedClock(now: now), handler).items(forceRefresh: false)
        return try makeRepository(container: container, clock: FixedClock(now: now.addingTimeInterval(60 * 60)), handler)
    }
    // </opt:database>

    private func makeRepository(_ handler: @escaping StubURLProtocol.Handler) throws -> DefaultSampleRepository {
        // <opt:database>
        try makeRepository(
            container: ModelContainerFactory.make(for: SampleStorage.models, inMemory: true),
            clock: SystemClock(),
            handler
        )
        // </opt:database>
        // <opt-else:database>
        //~ try DefaultSampleRepository(client: client(handler))
        // </opt-else:database>
    }
    // <opt:database>

    private func makeRepository(
        container: ModelContainer,
        clock: any AppClock,
        _ handler: @escaping StubURLProtocol.Handler
    ) throws -> DefaultSampleRepository {
        try DefaultSampleRepository(client: client(handler), modelContainer: container, clock: clock)
    }
    // </opt:database>

    private func client(_ handler: @escaping StubURLProtocol.Handler) throws -> APIClient {
        StubURLProtocol.handler = handler
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubURLProtocol.self]
        let baseURL = try #require(URL(string: "https://api.example.com/"))
        return APIClient(baseURL: baseURL, session: URLSession(configuration: configuration))
    }
}
// <opt:database>

private struct FixedClock: AppClock {
    let now: Date
}
// </opt:database>
