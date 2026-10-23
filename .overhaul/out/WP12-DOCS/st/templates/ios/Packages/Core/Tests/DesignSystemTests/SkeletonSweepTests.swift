@testable import DesignSystem
import SwiftUI
import Testing

struct SkeletonSweepTests {
    private let phone = CGSize(width: 393, height: 852)

    @Test(arguments: [LayoutDirection.leftToRight, .rightToLeft])
    func everyPlaceholderSamplesTheSameBand(direction: LayoutDirection) {
        let band = SkeletonBand(progress: 0.4, area: phone, layoutDirection: direction)
        let point = CGPoint(x: 180, y: 410)
        // Two placeholders covering `point` from different places, as a row and its neighbour's bone.
        let first = distancePastLeadingEdge(of: point, drawnFrom: CGPoint(x: 16, y: 380), on: band)
        let second = distancePastLeadingEdge(of: point, drawnFrom: CGPoint(x: 150, y: 402), on: band)

        #expect(abs(first - second) < 1e-9)
        #expect(abs(first - (band.phase(at: point) - band.start)) < 1e-9)
    }

    @Test(arguments: [0.0, 0.3, 0.7, 1.0])
    func rightToLeftMirrorsTheSweep(progress: Double) {
        let leftToRight = SkeletonBand(progress: progress, area: phone, layoutDirection: .leftToRight)
        let rightToLeft = SkeletonBand(progress: progress, area: phone, layoutDirection: .rightToLeft)

        #expect(rightToLeft.start == leftToRight.start)
        for point in [CGPoint.zero, CGPoint(x: 100, y: 500), CGPoint(x: phone.width, y: phone.height)] {
            let mirrored = CGPoint(x: phone.width - point.x, y: point.y)
            #expect(abs(rightToLeft.phase(at: point) - leftToRight.phase(at: mirrored)) < 1e-9)
        }
    }

    @Test(arguments: [LayoutDirection.leftToRight, .rightToLeft])
    func theBandIsOffTheWindowWhenASweepStartsAndEnds(direction: LayoutDirection) {
        let first = SkeletonBand(progress: 0, area: phone, layoutDirection: direction)
        let last = SkeletonBand(progress: 1, area: phone, layoutDirection: direction)
        let corners = [
            CGPoint.zero, CGPoint(x: phone.width, y: 0), CGPoint(x: 0, y: phone.height),
            CGPoint(x: phone.width, y: phone.height),
        ]
        for corner in corners {
            #expect(first.phase(at: corner) >= first.start + first.width)
            #expect(last.phase(at: corner) <= last.start)
        }
    }

    @Test func theBandMovesOnlyForwardAndRestsAtEitherEnd() {
        let start = Date(timeIntervalSinceReferenceDate: 0)
        let sweep = SkeletonSweep(area: phone, start: start)
        let edges = [0, 0.25, 0.5, 0.75, 1].map { fraction in
            sweep.band(at: start + fraction * sweep.duration, layoutDirection: .leftToRight).start
        }

        #expect(zip(edges, edges.dropFirst()).allSatisfy { $0 < $1 })
        #expect(sweep.band(at: start - 1, layoutDirection: .leftToRight).start == edges.first)
        #expect(sweep.band(at: start + sweep.duration + 1, layoutDirection: .leftToRight).start == edges.last)
    }

    @Test func aLargerWindowSweepsLongerUpToTheLimit() {
        let start = Date.now
        let small = SkeletonSweep(area: CGSize(width: 320, height: 480), start: start)
        let tablet = SkeletonSweep(area: CGSize(width: 744, height: 1133), start: start)
        let wall = SkeletonSweep(area: CGSize(width: 6000, height: 4000), start: start)

        #expect(small.duration == DesignTokens.Motion.skeletonSweep)
        #expect(tablet.duration > small.duration)
        #expect(wall.duration == DesignTokens.Motion.skeletonSweep * TimeInterval(DesignTokens.Motion.skeletonMaxSlowdown))
    }

    /// What the shader computes for `point` when the placeholder whose origin is at `origin` draws
    /// it: its position in that placeholder along the axis, plus the placeholder's offset.
    private func distancePastLeadingEdge(of point: CGPoint, drawnFrom origin: CGPoint, on band: SkeletonBand) -> CGFloat {
        let position = CGPoint(x: point.x - origin.x, y: point.y - origin.y)
        return position.x * band.axis.x + position.y * band.axis.y + band.offset(forOriginAt: origin)
    }
}
