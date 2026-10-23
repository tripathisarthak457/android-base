import SwiftUI

/// One pass of the shimmer band across a window: when it leaves, how long it takes to cross, and
/// the area it crosses.
nonisolated struct SkeletonSweep: Equatable, Sendable {
    let start: Date
    let duration: TimeInterval
    /// How far the window reaches in global coordinates.
    let area: CGSize

    init(area: CGSize, start: Date) {
        self.area = area
        self.start = start
        // A larger window has further to cross, so the sweep takes longer and the band keeps a
        // phone's speed, up to a limit, rather than racing across a desktop window.
        let slowdown = SkeletonBand.travel(across: area) / DesignTokens.Motion.skeletonReferenceTravel
        duration = DesignTokens.Motion.skeletonSweep
            * TimeInterval(min(max(slowdown, 1), DesignTokens.Motion.skeletonMaxSlowdown))
    }

    /// The band at `date`, on its eased way from wholly before the start edge to wholly past the far
    /// corner.
    func band(at date: Date, layoutDirection: LayoutDirection) -> SkeletonBand {
        let elapsed = min(max(date.timeIntervalSince(start) / duration, 0), 1)
        return SkeletonBand(
            progress: DesignTokens.Motion.skeletonEasing.value(at: elapsed),
            area: area,
            layoutDirection: layoutDirection
        )
    }
}

/// Where the shimmer band is in a window.
///
/// A point's phase is its distance along the band's direction, which is tilted down from the
/// horizontal and runs from the leading edge, so a right-to-left window mirrors the sweep. The band
/// covers the phases from `start` to `start + width`. Each placeholder finds its own points on it
/// from its position in the window, so the band crosses every placeholder as one.
nonisolated struct SkeletonBand: Equatable, Sendable {
    /// The direction the phase grows in, as a unit vector in the window's coordinates.
    let axis: CGPoint
    /// The phase of the band's leading edge.
    let start: CGFloat
    let width: CGFloat
    /// The phase of the window's origin: zero, or the window's width along the axis in
    /// right-to-left, where the sweep starts at the right edge.
    private let originPhase: CGFloat

    /// The band `progress` of the way across `area`: wholly before the start edge at 0, and wholly
    /// past the far corner at 1.
    init(progress: Double, area: CGSize, layoutDirection: LayoutDirection) {
        let mirrored = layoutDirection == .rightToLeft
        axis = CGPoint(x: mirrored ? -cos(Self.tilt) : cos(Self.tilt), y: sin(Self.tilt))
        originPhase = mirrored ? area.width * cos(Self.tilt) : 0
        width = DesignTokens.Motion.skeletonBandWidth
        start = -width + CGFloat(progress) * Self.travel(across: area)
    }

    /// How far the band's leading edge moves in one sweep across `area`.
    static func travel(across area: CGSize) -> CGFloat {
        area.width * cos(tilt) + area.height * sin(tilt) + DesignTokens.Motion.skeletonBandWidth
    }

    /// The phase of a point of the window.
    func phase(at point: CGPoint) -> CGFloat {
        originPhase + point.x * axis.x + point.y * axis.y
    }

    /// The distance past the band's leading edge of a placeholder's origin, at `origin` in the
    /// window. The shader adds it to the phase of each point measured from that origin.
    func offset(forOriginAt origin: CGPoint) -> CGFloat {
        phase(at: origin) - start
    }

    private static let tilt = CGFloat(DesignTokens.Motion.skeletonTiltDegrees) * .pi / 180
}
