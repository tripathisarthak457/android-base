import AppTesting
@testable import DesignSystem
import SwiftUI
import Testing

@MainActor
@Suite(.timeLimit(.minutes(1)))
struct SkeletonClockTests {
    private let clock = SkeletonClock()

    @Test func aPlaceholderOnScreenStartsASweepAcrossTheWindow() async {
        clock.area = CGSize(width: 393, height: 852)
        let placeholder = Task { await clock.attend() }
        await waitUntil { clock.sweep != nil }
        #expect(clock.sweep?.area == CGSize(width: 393, height: 852))

        placeholder.cancel()
        await placeholder.value
        #expect(clock.sweep == nil)
    }

    @Test func theClockStopsOnlyWhenTheLastPlaceholderLeaves() async {
        let first = Task { await clock.attend() }
        let second = Task { await clock.attend() }
        await waitUntil { clock.sweep != nil }

        first.cancel()
        await first.value
        // The second still attends, so a sweep is in flight or follows the pause.
        await waitUntil { clock.sweep != nil }

        second.cancel()
        await second.value
        #expect(clock.sweep == nil)
    }
}
