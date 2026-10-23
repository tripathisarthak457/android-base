import Foundation

/// A link into the app, `<scheme>://<tab>/<id>`: the tab to show and, for a list-detail tab, the
/// item to open in it.
///
///     baseapp://sample          the sample tab, at its root
///     baseapp://sample/42       the sample tab with item 42 open
public nonisolated struct DeepLink: Equatable, Sendable {
    /// The tab's id.
    public let tab: String

    /// The item to select in the tab, if the link names one.
    public let item: String?

    /// Reads `url`, or returns nil when it is not one of the app's links.
    public init?(url: URL, scheme: String) {
        guard url.scheme?.lowercased() == scheme.lowercased(), let host = url.host(), !host.isEmpty else {
            return nil
        }
        tab = host
        item = url.pathComponents.first { $0 != "/" }
    }
}
