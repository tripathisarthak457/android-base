import SwiftUI

/// How a sheet behaves and looks around its content. The defaults are Apple's: one full-height
/// detent, modal, and closed by a swipe down.
public nonisolated struct AppSheetOptions: Equatable, Sendable {
    /// The heights the sheet rests at. With more than one, people drag between them, and the
    /// sheet settles on the one its velocity carries it to.
    public var detents: Set<AppSheetDetent>

    public var modality: AppSheetModality

    public var contentInteraction: AppSheetContentInteraction

    /// Whether a swipe down or a tap outside can close the sheet at all. While it is false, a swipe
    /// down stretches the sheet and springs back, and only the sheet's own buttons close it. To ask
    /// before discarding changes, keep it true and guard the content with `appSheetDismissGuard`.
    public var dismissible: Bool

    public var material: AppSheetMaterial

    /// The grabber at the top edge. `.automatic` shows it when the sheet has more than one detent,
    /// which is when Apple advises one.
    public var grabber: Visibility

    public init(
        detents: Set<AppSheetDetent> = [.large],
        modality: AppSheetModality = .modal,
        contentInteraction: AppSheetContentInteraction = .automatic,
        dismissible: Bool = true,
        material: AppSheetMaterial = .automatic,
        grabber: Visibility = .automatic
    ) {
        self.detents = detents
        self.modality = modality
        self.contentInteraction = contentInteraction
        self.dismissible = dismissible
        self.material = material
        self.grabber = grabber
    }

    var fitsContent: Bool {
        detents.contains(.fitContent)
    }

    var grabberVisibility: Visibility {
        grabber == .automatic && detents.count > 1 ? .visible : grabber
    }

    var presentationContentInteraction: PresentationContentInteraction {
        switch (contentInteraction, modality) {
        case (.resizes, _):
            .resizes
        case (.scrolls, _), (.automatic, .nonModal):
            .scrolls
        case (.automatic, .modal):
            .automatic
        }
    }

    func presentationDetents(contentHeight: CGFloat?) -> Set<PresentationDetent> {
        Set(detents.map { $0.presentationDetent(contentHeight: contentHeight) })
    }

    /// Which of `detents` SwiftUI presents as `presented`, or nil for none of them.
    func detent(presentedAs presented: PresentationDetent, contentHeight: CGFloat?) -> AppSheetDetent? {
        detents.first { $0.presentationDetent(contentHeight: contentHeight) == presented }
    }
}
