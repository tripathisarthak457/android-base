import SwiftUI

/// Press feedback for filled buttons, chips and cards: the control shrinks a little under the
/// finger and springs back when it lifts. Nothing is drawn over it.
///
/// The dip runs on the `press` spring and the return on `release`, which carries the motion
/// style's bounce. With Reduce Motion on, the control dims instead of moving.
public struct ScalePressStyle: ButtonStyle {
    /// How far a control shrinks: buttons and chips, large surfaces such as cards, icon-sized
    /// controls.
    public enum Size: Sendable {
        case regular
        case large
        case icon
    }

    private let size: Size

    public init(_ size: Size = .regular) {
        self.size = size
    }

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .modifier(PressFeedback(kind: .scale(pressedScale), isPressed: configuration.isPressed))
    }

    private var pressedScale: CGFloat {
        switch size {
        case .regular: DesignTokens.Motion.pressScale
        case .large: DesignTokens.Motion.pressScaleLarge
        case .icon: DesignTokens.Motion.pressScaleIcon
        }
    }
}

extension ButtonStyle where Self == ScalePressStyle {
    /// Shrinks under the finger and springs back. For filled buttons, chips and cards.
    public static var scalePress: ScalePressStyle {
        ScalePressStyle()
    }

    public static func scalePress(_ size: ScalePressStyle.Size) -> ScalePressStyle {
        ScalePressStyle(size)
    }
}
