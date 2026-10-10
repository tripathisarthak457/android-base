import SwiftUI

/// A placeholder shaped like the content that is loading: that content drawn redacted, with one
/// highlight sweeping across all of it.
///
/// Give it the real layout filled with placeholder values, so nothing jumps when the content
/// arrives. One sweep runs for the whole skeleton, not one per row; it stops while the skeleton is
/// off screen and does not run at all with Reduce Motion on.
public struct AppSkeleton<Content: View>: View {
    private let content: Content

    @Environment(\.appTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isShimmering = false

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        content
            .foregroundStyle(theme.colors.skeleton)
            .redacted(reason: .placeholder)
            // Drawn over the whole skeleton rather than masked to its shapes: the content renders
            // once, and a List or ScrollView, which UIKit draws, is never needed as a mask.
            .overlay {
                if !reduceMotion {
                    shimmer
                }
            }
            .allowsHitTesting(false)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text("Loading", bundle: .module))
            .onAppear { isShimmering = true }
            .onDisappear { isShimmering = false }
    }

    private var shimmer: some View {
        GeometryReader { proxy in
            let band = proxy.size.width * 0.6
            LinearGradient(
                colors: [.clear, theme.colors.surface.opacity(0.7), .clear],
                startPoint: .leading,
                endPoint: .trailing
            )
            .frame(width: band)
            .offset(x: isShimmering ? proxy.size.width : -band)
            .animation(
                isShimmering ? .linear(duration: 1.4).repeatForever(autoreverses: false) : nil,
                value: isShimmering
            )
        }
        .clipped()
    }
}

#Preview("Skeleton") {
    AppSkeleton {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            ForEach(0..<6, id: \.self) { _ in
                VStack(alignment: .leading, spacing: AppSpacing.xxs) {
                    Text(verbatim: "A sample title that wraps")
                    Text(verbatim: "Its first line of body text, shown as a preview")
                }
            }
        }
        .padding()
    }
    .appTheme()
}
