import Foundation
import SwiftData

/// Reads and writes saved samples on the database's own executor, never on the main actor.
@ModelActor
actor SampleLocalStore {
    /// The saved samples in list order, if they were fetched after `date`; otherwise none.
    func items(fetchedAfter date: Date) throws -> [SampleItem] {
        let descriptor = FetchDescriptor<SampleItemRecord>(
            predicate: #Predicate { $0.fetchedAt > date },
            sortBy: [SortDescriptor(\.position)]
        )
        return try modelContext.fetch(descriptor).map(\.item)
    }

    func item(id: SampleItem.ID) throws -> SampleItem? {
        var descriptor = FetchDescriptor<SampleItemRecord>(predicate: #Predicate { $0.remoteID == id })
        descriptor.fetchLimit = 1
        return try modelContext.fetch(descriptor).first?.item
    }

    /// Replaces the saved list with `items` in one save, so a reader never sees half of each.
    func replaceAll(with items: [SampleItem], fetchedAt: Date) throws {
        try modelContext.delete(model: SampleItemRecord.self)
        for (position, item) in items.enumerated() {
            modelContext.insert(SampleItemRecord(item, position: position, fetchedAt: fetchedAt))
        }
        try modelContext.save()
    }
}
