import Foundation
import Persistence
import Testing

struct SettingsRepositoryTests {
    private let suite = "SettingsRepositoryTests.\(UUID().uuidString)"

    @Test func startsFromTheGeneratedDefaults() throws {
        let repository = SettingsRepository(defaults: try defaults(), hapticsDefault: false)
        #expect(repository.current == AppSettings(appearance: .system, hapticsEnabled: false, hasCompletedOnboarding: false))
    }

    @Test func everyWriteReachesTheStream() async throws {
        let repository = SettingsRepository(defaults: try defaults(), hapticsDefault: true)
        var changes = repository.changes().makeAsyncIterator()
        #expect(await changes.next()?.appearance == .system)

        repository.setAppearance(.dark)
        #expect(await changes.next()?.appearance == .dark)

        repository.setHapticsEnabled(false)
        #expect(await changes.next()?.hapticsEnabled == false)
    }

    @Test func choicesOutliveTheRepository() throws {
        let defaults = try defaults()
        SettingsRepository(defaults: defaults, hapticsDefault: true).setOnboardingCompleted()
        #expect(SettingsRepository(defaults: defaults, hapticsDefault: true).current.hasCompletedOnboarding)
    }

    private func defaults() throws -> UserDefaults {
        let defaults = try #require(UserDefaults(suiteName: suite))
        defaults.removePersistentDomain(forName: suite)
        return defaults
    }
}
