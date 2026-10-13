/// What a sheet is made of.
public nonisolated enum AppSheetStyle: Hashable, Sendable {
    /// The platform's sheet in the design style: from iOS 26 Liquid Glass, which turns opaque as the
    /// sheet grows to full height; before it, the elevated surface.
    case automatic

    /// The design style's elevated surface, opaque on every version.
    case solid

    /// A frosted material through which the colours behind show softly.
    case frosted

    /// Liquid Glass from iOS 26, and the thinnest material before it.
    case glass
}
