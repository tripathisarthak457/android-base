import Persistence
import SwiftData
import Testing

struct ModelContainerFactoryTests {
    @Test func anInMemoryContainerKeepsWhatIsSaved() throws {
        let container = try ModelContainerFactory.make(for: [Note.self], inMemory: true)
        let context = ModelContext(container)
        context.insert(Note(text: "Hello"))
        try context.save()
        #expect(try context.fetchCount(FetchDescriptor<Note>()) == 1)
    }
}

@Model
final class Note {
    var text: String

    init(text: String) {
        self.text = text
    }
}
