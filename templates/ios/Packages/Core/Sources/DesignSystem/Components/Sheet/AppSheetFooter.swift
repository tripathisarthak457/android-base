import SwiftUI

extension View {
    /// Pins `footer` to the sheet's visible bottom, above the keyboard: a sticky Save. A scroll
    /// view's content ends above it and scrolls under it. It is for a sheet that rests at heights
    /// of its own; a `.fitContent` sheet ends its stack with its actions instead.
    ///
    /// Applied after `appSheetHeader`, it stays while the header's stack pushes screens; applied
    /// before it, it belongs to the sheet's first screen alone.
    public func appSheetFooter<Footer: View>(@ViewBuilder _ footer: () -> Footer) -> some View {
        modifier(AppSheetFooter(footer: footer()))
    }
}

private struct AppSheetFooter<Footer: View>: ViewModifier {
    let footer: Footer

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26, *) {
            // A bar extends the scroll edge effect under itself, so content passing beneath it
            // softens as it does under the system's bars.
            content.safeAreaBar(edge: .bottom) {
                bar
            }
        } else {
            content.safeAreaInset(edge: .bottom, spacing: 0) {
                bar.background(.bar)
            }
        }
    }

    private var bar: some View {
        footer
            .padding(.horizontal, AppSpacing.gutter)
            .padding(.vertical, AppSpacing.md)
    }
}
