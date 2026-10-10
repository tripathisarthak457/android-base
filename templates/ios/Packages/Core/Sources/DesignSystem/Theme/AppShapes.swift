import SwiftUI

/// The design style's corners, outlines and container treatments.
public nonisolated struct AppShapes: Sendable {
    public let radiusXs: CGFloat
    public let radiusSm: CGFloat
    public let radiusMd: CGFloat
    public let radiusLg: CGFloat
    public let radiusXl: CGFloat
    public let radiusSheet: CGFloat
    /// A button's corner radius, or nil for a capsule.
    public let buttonRadius: CGFloat?
    /// A chip's corner radius, or nil for a capsule.
    public let chipRadius: CGFloat?
    public let card: CardTreatment
    public let field: FieldTreatment
    public let borderWidth: CGFloat
    public let borderWidthStrong: CGFloat
    /// How far an `offset` card's hard shadow sits from it.
    public let offsetShadow: CGFloat
    /// Whether section headers and small labels are set in capitals.
    public let uppercaseLabels: Bool

    init(style: DesignTokens.Style) {
        radiusXs = style.radiusXs
        radiusSm = style.radiusSm
        radiusMd = style.radiusMd
        radiusLg = style.radiusLg
        radiusXl = style.radiusXl
        radiusSheet = style.radiusSheet
        buttonRadius = style.buttonRadius
        chipRadius = style.chipRadius
        card = style.card
        field = style.field
        borderWidth = style.borderWidth
        borderWidthStrong = style.borderWidthStrong
        offsetShadow = style.offsetShadow
        uppercaseLabels = style.uppercaseLabels
    }

    /// The shape of a button: rounded by the style's button radius, or a capsule.
    public var button: AnyShape {
        buttonRadius.map { AnyShape(RoundedRectangle(cornerRadius: $0, style: .continuous)) } ?? AnyShape(Capsule())
    }
}
