import SwiftUI

/// What a control does under the finger, for `ScalePressStyle`, `DimPressStyle` and the styles that
/// draw a whole control but answer a press the same way, so every button of a kind feels the same.
struct PressFeedback: ViewModifier {
    enum Kind {
        /// Shrinks to the scale on the `press` spring and returns on `release`, which carries the
        /// motion style's bounce. With Reduce Motion on, it dims instead of moving.
        case scale(CGFloat)
        /// Dims at once and fades back when the finger lifts, as a system button does.
        case dim
    }

    let kind: Kind
    let isPressed: Bool

    @Environment(\.appTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        switch kind {
        case .scale(let scale):
            content
                .scaleEffect(isPressed && !reduceMotion ? scale : 1)
                .opacity(isPressed && reduceMotion ? DesignTokens.Motion.pressDimAlpha : 1)
                .animation(isPressed ? theme.springs.press : theme.springs.release, value: isPressed)
        case .dim:
            content
                .opacity(isPressed ? DesignTokens.Motion.pressDimAlpha : 1)
                .animation(isPressed ? nil : .easeOut(duration: DesignTokens.Motion.fadeShort), value: isPressed)
        }
    }
}
