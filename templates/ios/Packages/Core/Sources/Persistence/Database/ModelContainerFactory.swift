import SwiftData

/// Builds the app's one `ModelContainer`.
///
/// The app creates it once, at launch, with every data module's model types, and hands it to each
/// module's model actor. Never read or write models on the main actor: each data domain has a
/// `@ModelActor` of its own, so a large fetch or save cannot hold up a frame.
public enum ModelContainerFactory {
    /// - Parameter inMemory: tests and previews keep nothing on disk.
    public static func make(for models: [any PersistentModel.Type], inMemory: Bool = false) throws -> ModelContainer {
        let schema = Schema(models)
        let configuration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: inMemory)
        return try ModelContainer(for: schema, configurations: [configuration])
    }
}
