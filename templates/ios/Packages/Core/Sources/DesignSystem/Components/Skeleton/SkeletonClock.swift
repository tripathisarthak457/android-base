import SwiftUI

/// When the shimmer band of one window sweeps.
///
/// The band sweeps only while a placeholder on screen attends, and rests between sweeps for a
/// pause in which no frame is drawn; with no placeholder on screen the clock does not run at all.
/// `sweep` changes twice per period, never per frame: each placeholder places the band from it in
/// its own `TimelineView`.
@Observable
final class SkeletonClock {
    /// The sweep in flight; nil during the pause and while no placeholder attends.
    private(set) var sweep: SkeletonSweep?

    /// How far the window reaches in global coordinates, which is the area the band crosses. Read
    /// when a sweep starts, so a resize takes effect from the next one.
    @ObservationIgnored var area = CGSize.zero

    @ObservationIgnored private var attendance = 0
    @ObservationIgnored private var loop: Task<Void, Never>?

    /// Keeps the band sweeping until the calling task is cancelled, as a view's `task` is when the
    /// view leaves the screen.
    func attend() async {
        attendance += 1
        if loop == nil {
            loop = Task { [weak self] in
                await self?.run()
            }
        }
        // The iteration ends when the task is cancelled; nothing is ever sent.
        let (untilCancelled, continuation) = AsyncStream<Void>.makeStream()
        for await _ in untilCancelled {}
        continuation.finish()

        attendance -= 1
        if attendance == 0 {
            loop?.cancel()
            loop = nil
            sweep = nil
        }
    }

    // Checks for cancellation before each write: a loop cancelled just as its sleep ended must not
    // clear the sweep of the loop that a new placeholder has started since.
    private func run() async {
        while !Task.isCancelled {
            let sweep = SkeletonSweep(area: area, start: .now)
            self.sweep = sweep
            try? await Task.sleep(for: .seconds(sweep.duration))
            guard !Task.isCancelled else { return }
            self.sweep = nil
            try? await Task.sleep(for: .seconds(DesignTokens.Motion.skeletonPause))
        }
    }
}

/// Gives a window one `SkeletonClock`, sized to the window. A themed view inside another keeps
/// the window's clock, so every placeholder in the window sweeps with the same band.
struct SkeletonClockProvider: ViewModifier {
    @Environment(SkeletonClock.self) private var enclosing: SkeletonClock?
    @State private var clock = SkeletonClock()

    func body(content: Content) -> some View {
        if enclosing == nil {
            content
                .onGeometryChange(for: CGSize.self) { proxy in
                    let frame = proxy.frame(in: .global)
                    return CGSize(width: frame.maxX, height: frame.maxY)
                } action: { area in
                    clock.area = area
                }
                .environment(clock)
        } else {
            content
        }
    }
}
