import SwiftUI

/// Press feedback for plain buttons, icon buttons, toolbar actions and links: the label dims at
/// once and fades back when the finger lifts, as a system button does.
public struct DimPressStyle: ButtonStyle {
    public init() {}

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .modifier(PressFeedback(kind: .dim, isPressed: configuration.isPressed))
    }
}

extension ButtonStyle where Self == DimPressStyle {
    /// Dims under the finger. For plain and icon buttons, toolbar actions and links.
    public static var dimPress: DimPressStyle {
        DimPressStyle()
    }
}
