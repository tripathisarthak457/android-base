import SwiftUI

/// The colours of one status (success, warning, danger, info, neutral).
public nonisolated struct AppStatusColors: Sendable {
    /// The status hue as a fill: a badge, a destructive button.
    public let base: Color
    /// The hue as text or an icon on `subtle` or a surface, at 4.5:1 or better.
    public let content: Color
    /// A tinted background for a banner or a field in error.
    public let subtle: Color
    /// Text and icons on `base`.
    public let on: Color

    init(_ status: DesignTokens.Status) {
        base = status.base
        content = status.content
        subtle = status.subtle
        on = status.on
    }
}
