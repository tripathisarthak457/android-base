import AppCore
import Foundation
import Networking

/// The sample endpoints.
///
/// They call a public demo API rather than the app's backend, so the reference feature works on
/// the first run whatever the backend URL says. Absolute URLs leave the client's base URL alone.
struct SampleAPI: Sendable {
    let client: APIClient

    func items(forceRefresh: Bool) async -> AppResult<[SampleItem]> {
        await client.send(Self.list(forceRefresh: forceRefresh)).map { dtos in dtos.map(\.item) }
    }

    func item(id: SampleItem.ID) async -> AppResult<SampleItem> {
        await client.send(Self.detail(id: id)).map(\.item)
    }

    private static let posts = "https://jsonplaceholder.typicode.com/posts"

    static func list(forceRefresh: Bool) -> Endpoint<[SampleItemDTO]> {
        .get(posts, cachePolicy: forceRefresh ? .reloadIgnoringLocalCacheData : .useProtocolCachePolicy)
    }

    static func detail(id: SampleItem.ID) -> Endpoint<SampleItemDTO> {
        .get("\(posts)/\(id)")
    }
}
