import Foundation
import Navigation
import Testing

struct DeepLinkTests {
    @Test func readsTheTabAndTheItem() throws {
        let url = try #require(URL(string: "baseapp://sample/42"))
        let link = try #require(DeepLink(url: url, scheme: "baseapp"))
        #expect(link.tab == "sample")
        #expect(link.item == "42")
    }

    @Test func aLinkToATabHasNoItem() throws {
        let url = try #require(URL(string: "baseapp://settings"))
        #expect(DeepLink(url: url, scheme: "baseapp")?.item == nil)
    }

    @Test(arguments: ["https://example.com/sample/42", "other://sample", "baseapp:///"])
    func refusesLinksThatAreNotTheApps(text: String) throws {
        let url = try #require(URL(string: text))
        #expect(DeepLink(url: url, scheme: "baseapp") == nil)
    }
}
