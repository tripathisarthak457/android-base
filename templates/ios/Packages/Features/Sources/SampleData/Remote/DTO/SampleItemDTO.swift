import Foundation

/// A sample exactly as the server sends it.
struct SampleItemDTO: Decodable, Sendable {
    let id: Int
    let title: String
    let body: String

    /// The domain model, with the stray whitespace the server leaves around its text removed.
    var item: SampleItem {
        SampleItem(
            id: id,
            title: title.trimmingCharacters(in: .whitespacesAndNewlines),
            body: body.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }
}
