import SwiftUI

extension View {
    /// Draws this view as a placeholder for content that is loading: its text and images become
    /// bones in the skeleton colour, and one shimmer band crosses every placeholder in the window.
    ///
    /// Give it the real view filled with placeholder values, so nothing moves when the content
    /// arrives. Everything the view draws becomes bone, a background too, so apply it inside a
    /// card or a list row rather than around them, and never to a `List` or a `ScrollView`. While
    /// `isRedacted`, the view takes no input and VoiceOver skips it; `appLoadingRegion()` reads
    /// the loading region out once. With Reduce Motion on, the bones hold still.
    ///
    /// ```swift
    /// List(0..<12, id: \.self) { _ in
    ///     OrderRow(order: .placeholder)
    ///         .appRedacted()
    /// }
    /// .scrollDisabled(true)
    /// .appLoadingRegion()
    /// ```
    public func appRedacted(_ isRedacted: Bool = true) -> some View {
        modifier(AppRedacted(isRedacted: isRedacted))
    }

    /// Reads this view to VoiceOver as one "Loading" element in place of the placeholders inside it.
    /// Apply it once per list or page of `appRedacted(_:)` content, not to each row.
    public func appLoadingRegion() -> some View {
        accessibilityElement(children: .ignore)
            .accessibilityLabel(Text("Loading", bundle: .module))
    }
}

private struct AppRedacted: ViewModifier {
    let isRedacted: Bool

    @Environment(\.appTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.layoutDirection) private var layoutDirection
    @Environment(SkeletonClock.self) private var clock: SkeletonClock?

    func body(content: Content) -> some View {
        let sweeps = isRedacted && !reduceMotion
        let sweep = sweeps ? clock?.sweep : nil
        // `content` is built once, outside the timeline, so a frame of the sweep evaluates only
        // the bones' shader arguments, never the placeholder's own body.
        TimelineView(.animation(paused: sweep == nil)) { timeline in
            content
                .redacted(reason: isRedacted ? .placeholder : [])
                .modifier(SkeletonBones(
                    isEnabled: isRedacted,
                    band: sweep?.band(at: timeline.date, layoutDirection: layoutDirection),
                    bone: theme.colors.skeleton,
                    highlight: theme.colors.skeletonHighlight
                ))
        }
        .allowsHitTesting(!isRedacted)
        .accessibilityHidden(isRedacted)
        .task(id: sweeps) {
            if sweeps, let clock {
                await clock.attend()
            }
        }
    }
}

/// The skeleton shader over a redacted view: bones in the token colours, lit where `band` crosses.
private struct SkeletonBones: ViewModifier {
    let isEnabled: Bool
    let band: SkeletonBand?
    let bone: Color
    let highlight: Color

    func body(content: Content) -> some View {
        // The effect runs off the main actor, so it takes copies rather than this modifier.
        let function = Self.function
        let isEnabled = isEnabled
        let band = band
        let bone = bone
        let highlight = highlight
        content.visualEffect { effect, proxy in
            let origin = proxy.frame(in: .global).origin
            return effect.layerEffect(
                Shader(function: function, arguments: [
                    .float2(band?.axis ?? .zero),
                    .float(band?.offset(forOriginAt: origin) ?? 0),
                    .float(band?.width ?? 0),
                    .color(bone),
                    .color(highlight),
                    .float(Self.reach),
                ]),
                maxSampleOffset: CGSize(width: Self.reach, height: Self.reach),
                isEnabled: isEnabled
            )
        }
    }

    private static let function = ShaderLibrary.bundle(.module).appSkeleton

    /// How far around a pixel the shader looks for a bone's full opacity: past an antialiased
    /// edge, and well within the thinnest bone.
    private nonisolated static let reach: CGFloat = 1
}

#Preview("Light and dark") {
    HStack(spacing: 0) {
        RedactedSample()
            .appTheme()
            .environment(\.colorScheme, .light)
        RedactedSample()
            .appTheme(designStyle: .playful)
            .environment(\.colorScheme, .dark)
    }
}

#Preview("Right to left") {
    RedactedSample()
        .appTheme(designStyle: .editorial)
        .environment(\.layoutDirection, .rightToLeft)
}

#Preview("Largest text") {
    RedactedSample()
        .appTheme()
        .dynamicTypeSize(.accessibility5)
}

/// A loading list as a feature builds one. Reduce Motion cannot be set from code; the canvas's
/// device settings switch it.
private struct RedactedSample: View {
    @Environment(\.appTheme) private var theme

    var body: some View {
        List(0..<8, id: \.self) { _ in
            VStack(alignment: .leading, spacing: AppSpacing.xxs) {
                Text(verbatim: "A sample title that runs to a second line")
                    .font(theme.typography.headline)
                    .foregroundStyle(theme.colors.contentPrimary)
                Text(verbatim: "The first words of its body, as the list shows them.")
                    .font(theme.typography.subheadline)
                    .foregroundStyle(theme.colors.contentSecondary)
            }
            .appRedacted()
        }
        .scrollDisabled(true)
        .appLoadingRegion()
    }
}
