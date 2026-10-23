import Foundation
@testable import OnboardingFeature
import Persistence
import Testing

@MainActor
struct OnboardingStoreTests {
    private let suite = "OnboardingStoreTests.\(UUID().uuidString)"

    @Test func nextWalksThroughThePagesAndFinishesOnTheLast() throws {
        let settings = try makeSettings()
        let store = OnboardingStore(settings: settings, pageCount: 3)
        store.send(.next)
        store.send(.next)
        #expect(store.state.isLastPage)
        #expect(!settings.current.hasCompletedOnboarding)
        store.send(.next)
        #expect(settings.current.hasCompletedOnboarding)
    }

    @Test func skippingCountsAsFinishing() throws {
        let settings = try makeSettings()
        OnboardingStore(settings: settings, pageCount: 3).send(.skip)
        #expect(settings.current.hasCompletedOnboarding)
    }

    @Test func swipingStaysWithinThePages() throws {
        let store = OnboardingStore(settings: try makeSettings(), pageCount: 3)
        store.send(.pageChanged(7))
        #expect(store.state.page == 2)
        store.send(.pageChanged(-1))
        #expect(store.state.page == 0)
    }

    private func makeSettings() throws -> SettingsRepository {
        let defaults = try #require(UserDefaults(suiteName: suite))
        defaults.removePersistentDomain(forName: suite)
        return SettingsRepository(defaults: defaults, hapticsDefault: true)
    }
}
