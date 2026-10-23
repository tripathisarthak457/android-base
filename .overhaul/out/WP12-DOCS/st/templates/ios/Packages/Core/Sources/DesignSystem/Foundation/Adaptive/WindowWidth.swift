import SwiftUI

/// The width class of the window, from its width in points.
///
/// Decide layout from this, not from `horizontalSizeClass` alone, the device or the orientation.
/// An iPhone app in a resizable iPad or Mac window stays `.compact` in size class however wide
/// the window is dragged; its width class follows the window. The thresholds are the Android
/// template's, so both apps change layout at the same widths.
public nonisolated enum WindowWidth: Int, Comparable, CaseIterable, Sendable {
    /// Under 600 points: phones, and narrow windows. One column.
    case compact

    /// 600 to 839 points: small tablets, and iPad split views. Two columns where they help.
    case medium

    /// 840 to 1199 points: tablets in landscape, desktop windows.
    case expanded

    /// 1200 to 1599 points: wide desktop windows.
    case large

    /// 1600 points and wider.
    case extraLarge

    public init(points: CGFloat) {
        self = switch points {
        case ..<600: .compact
        case ..<840: .medium
        case ..<1200: .expanded
        case ..<1600: .large
        default: .extraLarge
        }
    }

    public static func < (lhs: WindowWidth, rhs: WindowWidth) -> Bool {
        lhs.rawValue < rhs.rawValue
    }
}

extension EnvironmentValues {
    /// The window's width class. Provided by `View.measuresWindowWidth()` at the root; `.compact`
    /// outside it.
    public nonisolated var appWindowWidth: WindowWidth {
        get { self[WindowWidthKey.self] }
        set { self[WindowWidthKey.self] = newValue }
    }
}

private nonisolated struct WindowWidthKey: EnvironmentKey {
    static let defaultValue = WindowWidth.compact
}

extension View {
    /// Measures this view's width and provides its width class as `\.appWindowWidth` to
    /// everything inside it. Apply it once, to the root of a window.
    ///
    /// Only a change of class reaches the environment, so resizing a window by a few points does
    /// not re-evaluate the views that read it. While someone drags a window's edge (iOS 26), a new
    /// class waits until they let go, so the layout changes once rather than at every threshold
    /// the drag crosses.
    public func measuresWindowWidth() -> some View {
        modifier(WindowWidthReader())
    }
}

private struct WindowWidthReader: ViewModifier {
    @State private var width = WindowWidth.compact
    @State private var pending: WindowWidth?
    @State private var isResizing = false

    func body(content: Content) -> some View {
        content
            .onGeometryChange(for: WindowWidth.self) { proxy in
                WindowWidth(points: proxy.size.width)
            } action: { newValue in
                if isResizing {
                    pending = newValue
                } else {
                    width = newValue
                }
            }
            .modifier(InteractiveResizeObserver(isResizing: $isResizing))
            .onChange(of: isResizing) { _, resizing in
                if !resizing, let pending {
                    width = pending
                    self.pending = nil
                }
            }
            .environment(\.appWindowWidth, width)
    }
}

private struct InteractiveResizeObserver: ViewModifier {
    @Binding var isResizing: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26, *) {
            content.onInteractiveResizeChange { resizing in
                isResizing = resizing
            }
        } else {
            content
        }
    }
}
