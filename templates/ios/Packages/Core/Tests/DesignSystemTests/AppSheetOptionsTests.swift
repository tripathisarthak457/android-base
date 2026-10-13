@testable import DesignSystem
import SwiftUI
import Testing

struct AppSheetOptionsTests {
    @Test func theDefaultsAreApples() {
        let options = AppSheetOptions()
        #expect(options.detents == [.large])
        #expect(options.modality == .modal)
        #expect(options.contentInteraction == .automatic)
        #expect(options.dismissible)
        #expect(options.style == .automatic)
    }

    @Test func theGrabberShowsWhenTheSheetCanBeResized() {
        #expect(AppSheetOptions().grabberVisibility == .automatic)
        #expect(AppSheetOptions(detents: [.medium, .large]).grabberVisibility == .visible)
        #expect(AppSheetOptions(detents: [.medium, .large], grabber: .hidden).grabberVisibility == .hidden)
        #expect(AppSheetOptions(grabber: .visible).grabberVisibility == .visible)
    }

    @Test func aFittedSheetIsPresentedAtItsContentsHeight() {
        let options = AppSheetOptions(detents: [.fitContent, .large])
        #expect(options.fitsContent)
        #expect(options.presentationDetents(contentHeight: 236) == [.height(236), .large])
        #expect(!AppSheetOptions(detents: [.medium, .large]).fitsContent)
    }

    @Test func whatSwiftUIPresentsMapsBackToTheDetentThatAskedForIt() {
        let options = AppSheetOptions(detents: [.fitContent, .medium, .large])
        #expect(options.detent(presentedAs: .height(236), contentHeight: 236) == .fitContent)
        #expect(options.detent(presentedAs: .medium, contentHeight: 236) == .medium)
        #expect(options.detent(presentedAs: .large, contentHeight: 236) == .large)
        #expect(options.detent(presentedAs: .fraction(0.3), contentHeight: 236) == nil)
    }
}
