import SwiftUI

/// A height a sheet rests at: the system's medium and large, a fraction of the space the sheet has,
/// a fixed or computed height, or the height of the sheet's own content.
public nonisolated struct AppSheetDetent: Hashable, Sendable {
    private nonisolated enum Height: Hashable, Sendable {
        case system(PresentationDetent)
        case fitContent
    }

    private let height: Height

    private init(_ height: Height) {
        self.height = height
    }

    /// The whole height below the status bar. On a phone the screen behind steps back as a card.
    public static let large = AppSheetDetent(.system(.large))

    /// About half the window. In compact height, such as an iPhone in landscape, the sheet skips it
    /// and rests at the full height.
    public static let medium = AppSheetDetent(.system(.medium))

    /// The height of the sheet's content, for a short choice, confirmation or form that does not
    /// scroll. The content is measured at its ideal height, so it is a plain stack with its title in
    /// it: a `NavigationStack` or a `List` has no height of its own to fit. Content taller than the
    /// window is cut off at the full height.
    public static let fitContent = AppSheetDetent(.fitContent)

    /// `fraction` of the height the sheet can take, from 0 to 1.
    public static func fraction(_ fraction: CGFloat) -> AppSheetDetent {
        AppSheetDetent(.system(.fraction(fraction)))
    }

    /// A fixed height in points. Dynamic Type does not scale it, so content with text in it fits
    /// better with `fitContent`.
    public static func height(_ height: CGFloat) -> AppSheetDetent {
        AppSheetDetent(.system(.height(height)))
    }

    /// A height that `detent` computes from the space the sheet has, such as a sliver that shows a
    /// header and nothing more.
    public static func custom<Detent: CustomPresentationDetent>(_ detent: Detent.Type) -> AppSheetDetent {
        AppSheetDetent(.system(.custom(detent)))
    }

    /// The detent SwiftUI presents, given the content's measured height.
    func presentationDetent(contentHeight: CGFloat?) -> PresentationDetent {
        switch height {
        case .system(let detent):
            return detent
        case .fitContent:
            // The content is measured in the sheet's first layout; until then the sheet needs a
            // height it can always take.
            guard let contentHeight else { return .medium }
            return .height(contentHeight)
        }
    }
}
