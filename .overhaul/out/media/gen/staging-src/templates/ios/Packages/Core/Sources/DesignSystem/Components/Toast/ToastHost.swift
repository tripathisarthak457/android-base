import SwiftUI

extension View {
    /// Draws `center`'s toasts over this view, at the top edge. Apply it once, to a window's root.
    ///
    /// A toast slides in on the `sheet` spring and can be swiped up to dismiss it early. Success and
    /// error toasts play their haptic when they arrive.
    public func toastHost(_ center: ToastCenter) -> some View {
        modifier(ToastHost(center: center))
    }
}

private struct ToastHost: ViewModifier {
    let center: ToastCenter

    @Environment(\.appTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        let hapticsEnabled = theme.hapticsEnabled
        content
            .overlay(alignment: .top) {
                ZStack {
                    if let toast = center.current {
                        ToastBanner(toast: toast) {
                            center.dismiss(toast)
                        }
                        .id(toast.id)
                        .transition(reduceMotion ? .opacity : .move(edge: .top).combined(with: .opacity))
                    }
                }
                .animation(theme.springs.sheet, value: center.current)
            }
            .sensoryFeedback(trigger: center.current) { _, toast in
                guard hapticsEnabled, let toast else { return nil }
                return switch toast.kind {
                case .info: nil
                case .success: .success
                case .error: .error
                }
            }
    }
}

private struct ToastBanner: View {
    let toast: Toast
    let onDismiss: () -> Void

    @Environment(\.appTheme) private var theme
    @State private var drag: CGFloat = 0
    @State private var arrived = false

    var body: some View {
        HStack(spacing: AppSpacing.sm) {
            Image(systemName: symbol)
                .foregroundStyle(tint)
                .symbolEffect(.bounce, value: arrived)
                .accessibilityHidden(true)
            Text(toast.message)
                .font(theme.typography.subheadline.weight(.medium))
                .multilineTextAlignment(.leading)
        }
        .padding(.horizontal, AppSpacing.lg)
        .padding(.vertical, AppSpacing.md)
        // No wider than a form, so in a wide window it stays a banner rather than a strip.
        .frame(maxWidth: AppLayout.formMaxWidth)
        .modifier(ToastSurface())
        .padding(.horizontal, AppSpacing.gutter)
        .padding(.top, AppSpacing.xs)
        .offset(y: drag)
        .gesture(swipeToDismiss)
        .onAppear { arrived = true }
        .accessibilityElement(children: .combine)
        .accessibilityAction(named: Text("Dismiss", bundle: .module), onDismiss)
    }

    private var swipeToDismiss: some Gesture {
        DragGesture(minimumDistance: AppSpacing.xs)
            .onChanged { value in
                let height = value.translation.height
                // Upward follows the finger; downward resists, since a toast has nowhere to go.
                drag = height < 0 ? height : height * 0.25
            }
            .onEnded { value in
                if value.predictedEndTranslation.height < -AppSizes.minTouchTarget {
                    onDismiss()
                } else {
                    withAnimation(theme.springs.release) {
                        drag = 0
                    }
                }
            }
    }

    private var symbol: String {
        switch toast.kind {
        case .info: "info.circle.fill"
        case .success: "checkmark.circle.fill"
        case .error: "exclamationmark.triangle.fill"
        }
    }

    private var tint: Color {
        switch toast.kind {
        case .info: theme.colors.info.base
        case .success: theme.colors.success.base
        case .error: theme.colors.danger.base
        }
    }
}

private struct ToastSurface: ViewModifier {
    @Environment(\.appTheme) private var theme

    func body(content: Content) -> some View {
        // <opt:liquidglass>
        AppGlassGroup {
            content
                .foregroundStyle(theme.colors.contentPrimary)
                .appGlass(in: Capsule())
        }
        // </opt:liquidglass>
        // <opt-else:liquidglass>
        //~ content
        //~     .foregroundStyle(theme.colors.contentInverse)
        //~     .background(theme.colors.surfaceInverse, in: Capsule())
        //~     .shadow(color: theme.colors.shadow, radius: DesignTokens.Elevation.overlay, y: DesignTokens.Elevation.raised)
        // </opt-else:liquidglass>
    }
}
