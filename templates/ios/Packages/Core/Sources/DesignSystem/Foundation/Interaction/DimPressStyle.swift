import SwiftUI

/// Press feedback for plain buttons, icon buttons, toolbar actions and links: the label dims at
/// once and fades back when the finger lifts, as a system button does.
public struct DimPressStyle: ButtonStyle {
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? DesignTokens.Motion.pressDimAlpha : 1)
            .animation(
                configuration.isPressed ? nil : .easeOut(duration: DesignTokens.Motion.fadeShort),
                value: configuration.isPressed
            )
    }
}

extension ButtonStyle where Self == DimPressStyle {
    /// Dims under the finger. For plain and icon buttons, toolbar actions and links.
    public static var dimPress: DimPressStyle {
        DimPressStyle()
    }
}
