import SwiftUI

/// Whether a sheet holds people's attention, or leaves the screen behind it in use.
public nonisolated enum AppSheetModality: Hashable, Sendable {
    /// The system's presentation: the screen behind is dimmed and takes no touches. Most sheets.
    case modal

    /// The screen behind stays undimmed and usable while the sheet rests at `detent` or a lower
    /// one, as Maps keeps its search sheet; above it the sheet is modal. Such a sheet is usually
    /// part of the screen rather than a task on top of it, so it is not `dismissible` either.
    case nonModal(upThrough: AppSheetDetent)

    func backgroundInteraction(contentHeight: CGFloat?) -> PresentationBackgroundInteraction {
        switch self {
        case .modal:
            .automatic
        case .nonModal(let detent):
            .enabled(upThrough: detent.presentationDetent(contentHeight: contentHeight))
        }
    }
}
