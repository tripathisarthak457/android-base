import AppCore
import Observation
import Persistence

/// The settings screen. Each change is written through at once; there is no Save button.
@MainActor
@Observable
final class SettingsStore {
    struct State: Equatable, Sendable {
        var appearance: Appearance
        var hapticsEnabled: Bool
        let version: String
    }

    enum Event: Sendable {
        case appeared
        case appearanceChanged(Appearance)
        case hapticsChanged(Bool)
    }

    private(set) var state: State

    private let settings: SettingsRepository
    private let tasks = TaskBag()

    init(settings: SettingsRepository, version: String) {
        self.settings = settings
        let current = settings.current
        state = State(appearance: current.appearance, hapticsEnabled: current.hapticsEnabled, version: version)
    }

    func send(_ event: Event) {
        switch event {
        case .appeared:
            followSettings()
        case .appearanceChanged(let appearance):
            state.appearance = appearance
            settings.setAppearance(appearance)
        case .hapticsChanged(let enabled):
            state.hapticsEnabled = enabled
            settings.setHapticsEnabled(enabled)
        }
    }

    /// Keeps the screen in step with changes made elsewhere, such as in another window on an iPad.
    private func followSettings() {
        tasks.exclusive(Self.following) { [weak self, settings] in
            for await current in settings.changes() {
                guard let self else { return }
                let updated = State(
                    appearance: current.appearance,
                    hapticsEnabled: current.hapticsEnabled,
                    version: state.version
                )
                if updated != state {
                    state = updated
                }
            }
        }
    }

    private static let following = "settings"
}
