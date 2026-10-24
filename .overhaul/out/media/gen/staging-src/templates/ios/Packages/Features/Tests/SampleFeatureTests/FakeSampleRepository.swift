import AppCore
import SampleData

/// Answers with whatever the test last told it to, and counts the questions.
actor FakeSampleRepository: SampleRepository {
    private var answer: AppResult<[SampleItem]>
    private(set) var requests = 0

    init(answer: AppResult<[SampleItem]>) {
        self.answer = answer
    }

    func answer(with answer: AppResult<[SampleItem]>) {
        self.answer = answer
    }

    func items(forceRefresh: Bool) async -> AppResult<[SampleItem]> {
        requests += 1
        return answer
    }

    func item(id: SampleItem.ID) async -> AppResult<SampleItem> {
        switch answer {
        case .success(let items):
            guard let item = items.first(where: { $0.id == id }) else {
                return .failure(.http(HTTPFailure(status: 404)))
            }
            return .success(item)
        case .failure(let error):
            return .failure(error)
        }
    }

    static let items = [
        SampleItem(id: 1, title: "One", body: "The first sample."),
        SampleItem(id: 2, title: "Two", body: "The second sample."),
    ]
}
