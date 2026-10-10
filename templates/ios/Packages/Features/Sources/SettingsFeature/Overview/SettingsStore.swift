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
        case appearanceChanged(Appearance)
        case hapticsChanged(Bool)
    }

    private(set) var state: State

    private let settings: SettingsRepository

    init(settings: SettingsRepository, version: String) {
        self.settings = settings
        let current = settings.current
        state = State(appearance: current.appearance, hapticsEnabled: current.hapticsEnabled, version: version)
    }

    func send(_ event: Event) {
        switch event {
        case .appearanceChanged(let appearance):
            state.appearance = appearance
            settings.setAppearance(appearance)
        case .hapticsChanged(let enabled):
            state.hapticsEnabled = enabled
            settings.setHapticsEnabled(enabled)
        }
    }
}
