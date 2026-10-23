import Foundation
import SwiftData

/// A sample as the database keeps it: its place in the list and when it was fetched.
@Model
final class SampleItemRecord {
    @Attribute(.unique) var remoteID: Int
    var title: String
    var body: String
    var position: Int
    var fetchedAt: Date

    init(_ item: SampleItem, position: Int, fetchedAt: Date) {
        remoteID = item.id
        title = item.title
        body = item.body
        self.position = position
        self.fetchedAt = fetchedAt
    }

    var item: SampleItem {
        SampleItem(id: remoteID, title: title, body: body)
    }
}
