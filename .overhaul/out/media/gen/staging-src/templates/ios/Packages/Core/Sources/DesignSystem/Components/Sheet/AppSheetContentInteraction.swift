/// What a swipe on a scroll view in a sheet does first: resize the sheet, or scroll the content.
/// People can always resize the sheet by its grabber.
public nonisolated enum AppSheetContentInteraction: Hashable, Sendable {
    /// Resizes a modal sheet, and scrolls a non-modal one, which stays up for its content.
    case automatic

    /// Grows the sheet to its largest detent before the content scrolls.
    case resizes

    /// Scrolls the content at every detent.
    case scrolls
}
