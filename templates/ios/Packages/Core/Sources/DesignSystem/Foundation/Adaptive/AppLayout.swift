import CoreGraphics

/// Widths that keep a layout usable in every window, in points. They are the Android template's, so
/// both apps make the same choice at the same size.
///
/// Cap content with these and centre it; never fix a view's width. The scroll view around capped
/// content stays as wide as the window, so it scrolls from anywhere in it.
public nonisolated enum AppLayout {
    /// The narrowest window the layouts are designed for: an iPad's slide-over and narrowest split.
    /// Anything that must fit every window, such as a context menu's preview, fits in this.
    public static let minimumWindowWidth: CGFloat = 320

    /// The widest a column of running text gets. Longer lines are hard to follow back to their start.
    public static let readableMaxWidth: CGFloat = 720

    /// The widest a form or a single focused flow gets, such as onboarding or sign-in, so its fields
    /// and its main button stay together in a wide window.
    public static let formMaxWidth: CGFloat = 480

    /// A supporting pane beside the content, such as an `.inspector`: the narrowest it gets, its
    /// width when the window leaves the choice open, and the widest, which is the Android template's
    /// supporting pane in a large window.
    public static let supportingPaneMinWidth: CGFloat = 280
    public static let supportingPaneIdealWidth: CGFloat = 320
    public static let supportingPaneMaxWidth: CGFloat = 412
}
