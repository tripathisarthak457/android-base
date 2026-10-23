@testable import DesignSystem
import SwiftUI
import Testing

struct AppSheetDetentTests {
    @Test func systemDetentsArePresentedAsThemselves() {
        #expect(AppSheetDetent.large.presentationDetent(contentHeight: 240) == .large)
        #expect(AppSheetDetent.medium.presentationDetent(contentHeight: nil) == .medium)
        #expect(AppSheetDetent.fraction(0.4).presentationDetent(contentHeight: 240) == .fraction(0.4))
        #expect(AppSheetDetent.height(120).presentationDetent(contentHeight: 240) == .height(120))
    }

    @Test func aFittedDetentIsPresentedAtTheContentsHeight() {
        #expect(AppSheetDetent.fitContent.presentationDetent(contentHeight: 236) == .height(236))
    }

    @Test func aFittedDetentRestsAtMediumUntilTheContentIsMeasured() {
        #expect(AppSheetDetent.fitContent.presentationDetent(contentHeight: nil) == .medium)
    }

    @Test func detentsAreEqualWhenTheyMeanTheSameHeight() {
        #expect(AppSheetDetent.height(120) == .height(120))
        #expect(AppSheetDetent.fitContent != .height(120))
        #expect(Set<AppSheetDetent>([.medium, .large, .medium]).count == 2)
    }
}
