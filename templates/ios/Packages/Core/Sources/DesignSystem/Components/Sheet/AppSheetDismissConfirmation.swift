import SwiftUI

extension View {
    /// Asks "Discard changes?" while `isEnabled`, before a swipe down, a tap outside or the close
    /// button of `appSheetHeader` closes the sheet this view is in: Discard calls `onDiscard`, which
    /// closes it, and Keep Editing leaves it open. Apply it inside the sheet's content, on the view
    /// that owns the changes, and enable it only while there is something to lose.
    public func appSheetDismissConfirmation(isEnabled: Bool, onDiscard: @escaping () -> Void) -> some View {
        modifier(AppSheetDismissConfirmation(
            isEnabled: isEnabled,
            title: Text("Discard changes?", bundle: .module),
            discardLabel: Text("Discard", bundle: .module),
            onDiscard: onDiscard
        ))
    }

    /// Asks `title` before the sheet closes while `isEnabled`, with `discardLabel` on the
    /// destructive button, as `appSheetDismissConfirmation(isEnabled:onDiscard:)` asks its question.
    public func appSheetDismissConfirmation(
        isEnabled: Bool,
        title: Text,
        discardLabel: Text,
        onDiscard: @escaping () -> Void
    ) -> some View {
        modifier(AppSheetDismissConfirmation(
            isEnabled: isEnabled,
            title: title,
            discardLabel: discardLabel,
            onDiscard: onDiscard
        ))
    }
}

private struct AppSheetDismissConfirmation: ViewModifier {
    let isEnabled: Bool
    let title: Text
    let discardLabel: Text
    let onDiscard: () -> Void

    @State private var asks = false

    @ViewBuilder
    func body(content: Content) -> some View {
        // dismissalConfirmationDialog exists only in the iOS 27 SDK, which only Swift 6.4 builds
        // against; `#available` alone fails to compile with an older SDK.
        #if compiler(>=6.4)
        if #available(iOS 27, *) {
            // The system asks on a swipe or a tap outside, and closes the sheet on Discard itself.
            // It never asks before a dismissal from code, so the close button still asks here.
            guarded(content, holdsInteractiveDismissal: false)
                .dismissalConfirmationDialog(title, shouldPresent: isEnabled) {
                    discardButton
                }
        } else {
            guarded(content, holdsInteractiveDismissal: true)
        }
        #else
        guarded(content, holdsInteractiveDismissal: true)
        #endif
    }

    private func guarded(_ content: Content, holdsInteractiveDismissal: Bool) -> some View {
        content
            .confirmationDialog(title, isPresented: $asks, titleVisibility: .visible) {
                discardButton
                Button(role: .cancel, action: {}) {
                    Text("Keep Editing", bundle: .module)
                }
            }
            .modifier(AppSheetDismissGuard(
                isEnabled: isEnabled,
                holdsInteractiveDismissal: holdsInteractiveDismissal,
                onAttempt: ask
            ))
    }

    private var discardButton: some View {
        Button(role: .destructive, action: onDiscard) {
            discardLabel
        }
    }

    private func ask() {
        asks = true
    }
}
