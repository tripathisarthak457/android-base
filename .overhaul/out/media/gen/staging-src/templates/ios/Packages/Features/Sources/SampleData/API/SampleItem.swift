/// What the app knows about a sample, with no trace of the wire format.
public struct SampleItem: Identifiable, Hashable, Sendable {
    public let id: Int
    public let title: String
    public let body: String

    public init(id: Int, title: String, body: String) {
        self.id = id
        self.title = title
        self.body = body
    }

    /// What a list row shows: the body on one line, cut after about 90 characters at a word.
    public var preview: String {
        let flat = body.split(whereSeparator: \.isNewline).joined(separator: " ")
        guard flat.count > Self.previewLength else { return flat }
        let cut = flat.prefix(Self.previewLength)
        let word = cut.lastIndex(of: " ").map { cut[..<$0] } ?? cut
        return String(word) + "…"
    }

    private static let previewLength = 90
}
