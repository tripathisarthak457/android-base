/// How a screen's primary content is doing.
public enum LoadState: Equatable, Sendable {
    /// Nothing requested yet.
    case idle

    /// The first load, with nothing to show yet. Show a skeleton shaped like the content.
    case loading

    /// A reload with content already on screen.
    case refreshing

    /// Loaded, with content.
    case loaded

    /// Loaded, and there was nothing.
    case empty

    /// Failed. The screen turns the error into words, because what to say depends on where it is.
    case failed(AppError)

    /// Whether content is on screen, including while it refreshes underneath.
    public var hasContent: Bool {
        self == .loaded || self == .refreshing
    }
}
