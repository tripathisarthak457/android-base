@testable import DesignSystem
import Testing

struct AppChoiceSheetTests {
    private struct DistanceUnit: Hashable {
        let name: String
        let symbol: String
    }

    private let units = [
        DistanceUnit(name: "Kilometres", symbol: "km"),
        DistanceUnit(name: "Miles", symbol: "mi"),
        DistanceUnit(name: "Nautical miles", symbol: "nmi"),
    ]

    private func search(_ query: String) -> [String] {
        AppChoiceList<DistanceUnit>
            .choices(units, matching: query, label: { $0.name }, supportingText: { $0.symbol })
            .map(\.name)
    }

    @Test func aBlankQueryShowsEveryChoice() {
        #expect(search("") == ["Kilometres", "Miles", "Nautical miles"])
        #expect(search("  ") == ["Kilometres", "Miles", "Nautical miles"])
    }

    @Test func theLabelMatchesWhateverItsCase() {
        #expect(search("MILES") == ["Miles", "Nautical miles"])
    }

    @Test func theSupportingTextMatchesToo() {
        #expect(search("nmi") == ["Nautical miles"])
    }

    @Test func accentsAreIgnored() {
        let languages = ["Español", "Français", "Deutsch"]
        let found = AppChoiceList<String>.choices(languages, matching: "francais", label: { $0 }, supportingText: nil)
        #expect(found == ["Français"])
    }

    @Test func nothingMatchesAnUnknownWord() {
        #expect(search("furlongs").isEmpty)
    }
}
