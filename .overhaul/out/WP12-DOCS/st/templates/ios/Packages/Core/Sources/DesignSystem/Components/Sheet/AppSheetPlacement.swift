/// Where a sheet sits in a regular-width window, such as an iPad's. On a phone every sheet comes
/// up from the bottom edge, whatever its placement.
public nonisolated enum AppSheetPlacement: Hashable, Sendable {
    /// The system's choice. Before iOS 27 that is always a centred form sheet.
    case automatic

    /// A centred form sheet, even where the system would place the sheet elsewhere.
    case center

    /// A panel along the leading edge, from iOS 27, that leaves the content beside it in view; with
    /// `.nonModal` it stays usable, as Maps keeps its search panel. Earlier versions place the sheet
    /// automatically.
    case leading

    /// A panel along the trailing edge, from iOS 27, as `leading` is along the other.
    case trailing
}
