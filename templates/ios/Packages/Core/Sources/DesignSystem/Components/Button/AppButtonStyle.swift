import SwiftUI

/// The design system's buttons, as a style for SwiftUI's own `Button`:
///
///     Button(action: save) { Text("Save", bundle: .module) }
///         .buttonStyle(.app(.filled))
///
/// Filled, tinted and destructive buttons shrink under the finger and spring back; plain buttons
/// dim. Each keeps a 44-point touch target whatever its drawn height, and grows with Dynamic Type.
public struct AppButtonStyle: ButtonStyle {
    public enum Kind: Sendable {
        /// The one primary action of a screen.
        case filled
        /// A secondary action that still deserves weight.
        case tinted
        /// An action that deletes or cannot be undone.
        case destructive
        /// A low-emphasis action, drawn as text.
        case plain
    }

    public enum Size: Sendable {
        case small
        case medium
        case large
    }

    private let kind: Kind
    private let size: Size

    public init(_ kind: Kind = .filled, size: Size = .medium) {
        self.kind = kind
        self.size = size
    }

    public func makeBody(configuration: Configuration) -> some View {
        AppButtonBody(label: configuration.label, isPressed: configuration.isPressed, kind: kind, size: size)
    }
}

extension ButtonStyle where Self == AppButtonStyle {
    public static func app(_ kind: AppButtonStyle.Kind = .filled, size: AppButtonStyle.Size = .medium) -> AppButtonStyle {
        AppButtonStyle(kind, size: size)
    }
}

private struct AppButtonBody: View {
    let label: ButtonStyleConfiguration.Label
    let isPressed: Bool
    let kind: AppButtonStyle.Kind
    let size: AppButtonStyle.Size

    @Environment(\.appTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let shape = theme.shapes.button
        label
            .font(size == .small ? theme.typography.subheadline.weight(.semibold) : theme.typography.headline)
            .foregroundStyle(foreground)
            .lineLimit(2)
            .multilineTextAlignment(.center)
            .padding(.horizontal, size == .small ? AppSpacing.md : AppSpacing.lg)
            .padding(.vertical, AppSpacing.xs)
            .frame(minWidth: AppSizes.minTouchTarget, minHeight: height)
            .background(background, in: shape)
            .contentShape(shape)
            .scaleEffect(scaled ? DesignTokens.Motion.pressScale : 1)
            .opacity(dimmed ? DesignTokens.Motion.pressDimAlpha : 1)
            .animation(isPressed ? theme.springs.press : theme.springs.release, value: isPressed)
            // A small button is drawn shorter than a finger; the padding below is untinted and
            // still takes the touch.
            .padding(.vertical, max(0, (AppSizes.minTouchTarget - height) / 2))
            .contentShape(.rect)
            .hoverEffect(.highlight)
    }

    private var height: CGFloat {
        switch size {
        case .small: AppSizes.buttonSmall
        case .medium: AppSizes.buttonMedium
        case .large: AppSizes.buttonLarge
        }
    }

    private var scaled: Bool {
        isPressed && kind != .plain && !reduceMotion
    }

    private var dimmed: Bool {
        isPressed && (kind == .plain || reduceMotion)
    }

    private var foreground: Color {
        guard isEnabled else { return theme.colors.contentDisabled }
        return switch kind {
        case .filled: theme.colors.onAccent
        case .tinted, .plain: theme.colors.accentContent
        case .destructive: theme.colors.danger.on
        }
    }

    private var background: Color {
        if kind == .plain {
            return .clear
        }
        guard isEnabled else { return theme.colors.fill }
        return switch kind {
        case .filled: theme.colors.accent
        case .tinted: theme.colors.accentSubtle
        case .destructive: theme.colors.danger.base
        case .plain: .clear
        }
    }
}

#Preview("Buttons") {
    VStack(spacing: AppSpacing.md) {
        Button {} label: { Text(verbatim: "Filled") }
            .buttonStyle(.app(.filled, size: .large))
        Button {} label: { Text(verbatim: "Tinted") }
            .buttonStyle(.app(.tinted))
        Button {} label: { Text(verbatim: "Destructive") }
            .buttonStyle(.app(.destructive))
        Button {} label: { Text(verbatim: "Plain") }
            .buttonStyle(.app(.plain, size: .small))
        Button {} label: { Text(verbatim: "Disabled") }
            .buttonStyle(.app(.filled))
            .disabled(true)
    }
    .padding()
    .appTheme()
}
