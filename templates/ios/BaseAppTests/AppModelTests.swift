@testable import BaseApp
import Foundation
import Testing

@MainActor
struct AppModelTests {
    private let services = AppServices(defaults: UserDefaults(suiteName: "AppModelTests") ?? .standard)

    @Test func everyTabHasASymbolOfItsOwn() {
        let symbols = AppTab.allCases.map(\.systemImage)
        #expect(Set(symbols).count == symbols.count)
    }

    @Test func aDeepLinkOpensItsTabWithItsItem() throws {
        let model = AppModel(services: services)
        let tab = try #require(AppTab.allCases.last)
        let url = try #require(URL(string: "baseapp://\(tab.rawValue)/42"))

        model.open(url)

        #expect(model.selection == tab)
        #expect(model.router(for: tab).selection == "42")
    }

    @Test func aLinkToAnotherAppIsIgnored() throws {
        let model = AppModel(services: services)
        let first = model.selection
        model.open(try #require(URL(string: "https://example.com/settings")))
        #expect(model.selection == first)
    }

    @Test func reselectingATabGoesBackToItsRoot() {
        let model = AppModel(services: services)
        let router = model.router(for: model.selection)
        router.push(1)
        router.selection = "7"

        model.reselect(model.selection)

        #expect(router.depth == 0)
        #expect(router.selection == nil)
    }
}
