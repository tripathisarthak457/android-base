import SwiftData

/// What the sample data module keeps on the device.
public enum SampleStorage {
    /// The model types to put in the app's `ModelContainer`.
    public static var models: [any PersistentModel.Type] {
        [SampleItemRecord.self]
    }
}
