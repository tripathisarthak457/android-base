import SwiftUI

/// How a sheet behaves and looks around its content. The defaults are Apple's: one full-height
/// detent, modal, and closed by a swipe down.
public nonisolated struct AppSheetOptions: Sendable {
    /// The heights the sheet rests at. With more than one, people drag between them, and the
    /// sheet settles on the one its velocity carries it to.
    public var detents: Set<AppSheetDetent>

    public var modality: AppSheetModality

    /// Whether a swipe on a scroll view in the sheet first resizes the sheet up to its largest
    /// detent (`.automatic`, `.resizes`) or first scrolls (`.scrolls`). A non-modal sheet with a
    /// list scrolls first. People can always resize it by the grabber.
    public var contentInteraction: PresentationContentInteraction

    /// Whether a swipe down or a tap outside closes the sheet. While it is false, a swipe down
    /// stretches the sheet and springs back, and only the sheet's own buttons close it.
    public var dismissible: Bool

    public var style: AppSheetStyle

    /// The grabber at the top edge. `.automatic` shows it when the sheet has more than one detent,
    /// which is when Apple advises one.
    public var grabber: Visibility

    public init(
        detents: Set<AppSheetDetent> = [.large],
        modality: AppSheetModality = .modal,
        contentInteraction: PresentationContentInteraction = .automatic,
        dismissible: Bool = true,
        style: AppSheetStyle = .automatic,
        grabber: Visibility = .automatic
    ) {
        self.detents = detents
        self.modality = modality
        self.contentInteraction = contentInteraction
        self.dismissible = dismissible
        self.style = style
        self.grabber = grabber
    }

    var fitsContent: Bool {
        detents.contains(.fitContent)
    }

    var grabberVisibility: Visibility {
        grabber == .automatic && detents.count > 1 ? .visible : grabber
    }

    func presentationDetents(contentHeight: CGFloat?) -> Set<PresentationDetent> {
        Set(detents.map { $0.presentationDetent(contentHeight: contentHeight) })
    }

    /// Which of `detents` SwiftUI presents as `presented`, or nil for none of them.
    func detent(presentedAs presented: PresentationDetent, contentHeight: CGFloat?) -> AppSheetDetent? {
        detents.first { $0.presentationDetent(contentHeight: contentHeight) == presented }
    }
}
