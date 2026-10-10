import Foundation
import Persistence
@testable import SampleData
import SwiftData
import Testing

struct SampleLocalStoreTests {
    private let items = [
        SampleItem(id: 1, title: "One", body: "First"),
        SampleItem(id: 2, title: "Two", body: "Second"),
    ]

    @Test func keepsTheListInOrder() async throws {
        let store = SampleLocalStore(modelContainer: try ModelContainerFactory.make(for: SampleStorage.models, inMemory: true))
        let fetched = Date(timeIntervalSince1970: 1_000)
        try await store.replaceAll(with: items, fetchedAt: fetched)

        #expect(try await store.items(fetchedAfter: fetched.addingTimeInterval(-1)) == items)
        #expect(try await store.items(fetchedAfter: fetched.addingTimeInterval(1)).isEmpty)
        #expect(try await store.item(id: 2) == items[1])
    }

    @Test func replacingDropsWhatTheNewListDoesNotHave() async throws {
        let store = SampleLocalStore(modelContainer: try ModelContainerFactory.make(for: SampleStorage.models, inMemory: true))
        try await store.replaceAll(with: items, fetchedAt: .now)
        try await store.replaceAll(with: [items[1]], fetchedAt: .now)
        #expect(try await store.items(fetchedAfter: .distantPast) == [items[1]])
        #expect(try await store.item(id: 1) == nil)
    }
}
