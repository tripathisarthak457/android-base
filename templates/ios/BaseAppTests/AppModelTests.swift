@testable import BaseApp
import Foundation
import Testing

@MainActor
struct AppModelTests {
    @Test func everyTabHasASymbolOfItsOwn() {
        let symbols = AppTab.allCases.map(\.systemImage)
        #expect(Set(symbols).count == symbols.count)
    }

    @Test func aDeepLinkOpensItsTabWithItsItem() throws {
        let model = try makeModel()
        let tab = try #require(AppTab.allCases.last)
        let url = try #require(URL(string: "baseapp://\(tab.rawValue)/42"))

        model.open(url)

        #expect(model.selection == tab)
        #expect(model.router(for: tab).selection == "42")
    }

    @Test func aLinkToAnotherAppIsIgnored() throws {
        let model = try makeModel()
        let first = model.selection
        model.open(try #require(URL(string: "https://example.com/settings")))
        #expect(model.selection == first)
    }

    @Test func reselectingATabGoesBackToItsRoot() throws {
        let model = try makeModel()
        let router = model.router(for: model.selection)
        router.push(1)
        router.selection = "7"

        model.reselect(model.selection)

        #expect(router.depth == 0)
        #expect(router.selection == nil)
    }

    /// A window's model over services that share nothing with the app or with other tests.
    private func makeModel() throws -> AppModel {
        let suite = "AppModelTests.\(UUID().uuidString)"
        let defaults = try #require(UserDefaults(suiteName: suite))
        defaults.removePersistentDomain(forName: suite)
        // <opt:sample&database>
        return AppModel(services: AppServices(defaults: defaults, keepsDataInMemory: true))
        // </opt:sample&database>
        // <opt-else:sample&database>
        //~ return AppModel(services: AppServices(defaults: defaults))
        // </opt-else:sample&database>
    }
}
