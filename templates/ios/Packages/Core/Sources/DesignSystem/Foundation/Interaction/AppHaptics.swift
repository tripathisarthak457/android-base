import SwiftUI

extension View {
    /// Plays `feedback` when `trigger` changes, unless the app's haptics are off.
    ///
    /// Like iOS itself, the app is silent on plain taps. Use this for the moments that earn a
    /// haptic: `.selection` when a segment, picker or slider step changes, `.impact` when a pull
    /// or a swipe crosses its threshold, and `.success` or `.error` for an outcome.
    public func appHaptic<Trigger: Equatable>(_ feedback: SensoryFeedback, trigger: Trigger) -> some View {
        modifier(AppHapticModifier(feedback: feedback, trigger: trigger))
    }
}

private struct AppHapticModifier<Trigger: Equatable>: ViewModifier {
    let feedback: SensoryFeedback
    let trigger: Trigger

    @Environment(\.appTheme) private var theme

    func body(content: Content) -> some View {
        let enabled = theme.hapticsEnabled
        content.sensoryFeedback(feedback, trigger: trigger) { _, _ in enabled }
    }
}
