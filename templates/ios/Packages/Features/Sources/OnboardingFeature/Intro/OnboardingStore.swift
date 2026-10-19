import Observation
import Persistence

/// The walkthrough: which page is showing, and finishing it.
@MainActor
@Observable
final class OnboardingStore {
    struct State: Equatable, Sendable {
        var page = 0
        let pageCount: Int

        var isLastPage: Bool {
            page == pageCount - 1
        }
    }

    enum Event: Sendable {
        case pageChanged(Int)
        case next
        case skip
    }

    private(set) var state: State

    private let settings: SettingsRepository

    init(settings: SettingsRepository, pageCount: Int) {
        self.settings = settings
        state = State(pageCount: pageCount)
    }

    func send(_ event: Event) {
        switch event {
        case .pageChanged(let page):
            state.page = min(max(page, 0), state.pageCount - 1)
        case .next where state.isLastPage, .skip:
            // Skipping counts as finishing, so the walkthrough never comes back on the next launch.
            settings.setOnboardingCompleted()
        case .next:
            state.page += 1
        }
    }
}
