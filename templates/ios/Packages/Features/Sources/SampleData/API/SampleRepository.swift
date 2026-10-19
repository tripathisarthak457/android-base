import AppCore

/// Everything the app can ask about samples. A protocol, so a store's tests use a fake and never
/// touch the network.
public protocol SampleRepository: Sendable {
    /// The samples, in the server's order.
    ///
    /// - Parameter forceRefresh: what pull to refresh passes. Otherwise a recent answer may be
    ///   reused instead of asking the server again.
    func items(forceRefresh: Bool) async -> AppResult<[SampleItem]>

    func item(id: SampleItem.ID) async -> AppResult<SampleItem>
}
