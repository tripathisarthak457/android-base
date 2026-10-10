import Foundation
import Persistence
@testable import SettingsFeature
import Testing

@MainActor
struct SettingsStoreTests {
    private let suite = "SettingsStoreTests.\(UUID().uuidString)"

    @Test func startsFromWhatIsSaved() throws {
        let settings = try makeSettings()
        settings.setAppearance(.dark)
        let store = SettingsStore(settings: settings, version: "1.0.0 (1)")
        #expect(store.state.appearance == .dark)
        #expect(store.state.hapticsEnabled)
    }

    @Test func everyChangeIsWrittenThrough() throws {
        let settings = try makeSettings()
        let store = SettingsStore(settings: settings, version: "1.0.0 (1)")
        store.send(.appearanceChanged(.light))
        store.send(.hapticsChanged(false))
        #expect(settings.current.appearance == .light)
        #expect(!settings.current.hapticsEnabled)
        #expect(store.state.appearance == .light)
    }

    @Test func aChangeMadeInAnotherWindowReachesTheScreen() async throws {
        let settings = try makeSettings()
        let store = SettingsStore(settings: settings, version: "1.0.0 (1)")
        store.send(.appeared)

        settings.setHapticsEnabled(false)

        let deadline = ContinuousClock.now + .seconds(2)
        while store.state.hapticsEnabled, ContinuousClock.now < deadline {
            try await Task.sleep(for: .milliseconds(5))
        }
        #expect(!store.state.hapticsEnabled)
    }

    private func makeSettings() throws -> SettingsRepository {
        let defaults = try #require(UserDefaults(suiteName: suite))
        defaults.removePersistentDomain(forName: suite)
        return SettingsRepository(defaults: defaults, hapticsDefault: true)
    }
}
