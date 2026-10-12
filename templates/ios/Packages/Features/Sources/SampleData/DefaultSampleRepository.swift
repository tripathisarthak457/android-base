import AppCore
import Foundation
import Networking
// <opt:database>
import OSLog
import SwiftData
// </opt:database>

// <opt:database>
/// Samples from the network, kept on the device.
///
/// A saved list younger than five minutes is answered without a request, and when the server cannot
/// be reached the saved list of any age stands in for an error. A refresh always asks the server and
/// reports its failure, so the screen can say that what it shows is not newer.
// </opt:database>
// <opt-else:database>
//~ /// Samples from the network. URLSession's cache answers a repeated request while the server's
//~ /// cache headers allow it; a refresh skips the cache.
// </opt-else:database>
public final class DefaultSampleRepository: SampleRepository {
    private let api: SampleAPI
    // <opt:database>
    private let local: SampleLocalStore
    private let clock: any AppClock
    // </opt:database>

    // <opt:database>
    public init(client: APIClient, modelContainer: ModelContainer, clock: any AppClock = SystemClock()) {
        api = SampleAPI(client: client)
        local = SampleLocalStore(modelContainer: modelContainer)
        self.clock = clock
    }
    // </opt:database>
    // <opt-else:database>
    //~ public init(client: APIClient) {
    //~     api = SampleAPI(client: client)
    //~ }
    // </opt-else:database>

    public func items(forceRefresh: Bool) async -> AppResult<[SampleItem]> {
        // <opt:database>
        if !forceRefresh,
           let saved = try? await local.items(fetchedAfter: clock.now.addingTimeInterval(-Self.freshFor)),
           !saved.isEmpty {
            return .success(saved)
        }
        let result = await api.items(forceRefresh: forceRefresh)
        switch result {
        case .success(let items):
            do {
                try await local.replaceAll(with: items, fetchedAt: clock.now)
            } catch {
                Self.log.error("Could not save the samples: \(String(describing: error), privacy: .public)")
            }
            return result
        case .failure(let error) where forceRefresh || error == .cancelled:
            return result
        case .failure:
            if let saved = try? await local.items(fetchedAfter: .distantPast), !saved.isEmpty {
                return .success(saved)
            }
            return result
        }
        // </opt:database>
        // <opt-else:database>
        //~ await api.items(forceRefresh: forceRefresh)
        // </opt-else:database>
    }

    public func item(id: SampleItem.ID) async -> AppResult<SampleItem> {
        // <opt:database>
        if let saved = try? await local.item(id: id) {
            return .success(saved)
        }
        // </opt:database>
        return await api.item(id: id)
    }
    // <opt:database>

    /// How long a saved list is shown without asking the server again.
    private static let freshFor: TimeInterval = 5 * 60

    private static let log = Log.logger(category: "SampleData")
    // </opt:database>
}
