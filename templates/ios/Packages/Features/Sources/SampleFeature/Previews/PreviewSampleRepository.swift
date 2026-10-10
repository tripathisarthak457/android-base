import AppCore
import DesignSystem
import SampleData

/// Samples for previews, answered at once and without the network.
nonisolated struct PreviewSampleRepository: SampleRepository {
    var samples = Self.examples

    func items(forceRefresh: Bool) async -> AppResult<[SampleItem]> {
        .success(samples)
    }

    func item(id: SampleItem.ID) async -> AppResult<SampleItem> {
        guard let sample = samples.first(where: { $0.id == id }) else {
            return .failure(.http(HTTPFailure(status: 404)))
        }
        return .success(sample)
    }

    static let examples = [
        SampleItem(
            id: 1,
            title: "Springs, not curves",
            body: "Every animation in the app runs on a spring from the design tokens, so it can be interrupted and keeps its speed."
        ),
        SampleItem(
            id: 2,
            title: "One list, two layouts",
            body: "On a phone the detail is pushed over the list. In a wide window both columns stay on screen."
        ),
        SampleItem(
            id: 3,
            title: "Offline first",
            body: "With the database feature on, the list you saw last is still there when the network is not."
        ),
    ]
}

extension SampleListStore {
    /// A store for previews, online and never told otherwise.
    static func preview(_ repository: any SampleRepository = PreviewSampleRepository()) -> SampleListStore {
        SampleListStore(repository: repository, connectivity: { AsyncStream { _ in } }, toasts: ToastCenter())
    }
}
