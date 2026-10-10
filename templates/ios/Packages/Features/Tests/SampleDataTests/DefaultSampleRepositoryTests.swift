import AppCore
import Foundation
import Networking
@testable import SampleData
import Testing
// <opt:database>
import Persistence
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
        let online = RequestCounter()
        let repository = try makeRepository { _ in
            online.increment()
            return online.value == 1
                ? .answer(#"[{"id": 1, "title": "One", "body": "First"}]"#)
                : .failure(URLError(.notConnectedToInternet))
        }
        _ = await repository.items(forceRefresh: false)
        let offline = await repository.items(forceRefresh: true)
        #expect(try offline.get() == [SampleItem(id: 1, title: "One", body: "First")])
    }
    // </opt:database>

    private func makeRepository(_ handler: @escaping StubURLProtocol.Handler) throws -> DefaultSampleRepository {
        StubURLProtocol.handler = handler
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubURLProtocol.self]
        let baseURL = try #require(URL(string: "https://api.example.com/"))
        let client = APIClient(baseURL: baseURL, session: URLSession(configuration: configuration))
        // <opt:database>
        let container = try ModelContainerFactory.make(for: SampleStorage.models, inMemory: true)
        return DefaultSampleRepository(client: client, modelContainer: container)
        // </opt:database>
        // <opt-else:database>
        //~ return DefaultSampleRepository(client: client)
        // </opt-else:database>
    }
}
