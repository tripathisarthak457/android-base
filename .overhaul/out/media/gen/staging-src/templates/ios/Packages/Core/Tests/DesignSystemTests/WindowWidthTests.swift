import DesignSystem
import Testing

struct WindowWidthTests {
    @Test(arguments: [
        (320.0, WindowWidth.compact), (599.0, .compact), (600.0, .medium), (839.0, .medium),
        (840.0, .expanded), (1199.0, .expanded), (1200.0, .large), (1599.0, .large), (1600.0, .extraLarge),
    ])
    func classesChangeAtTheAndroidThresholds(points: Double, expected: WindowWidth) {
        #expect(WindowWidth(points: points) == expected)
    }

    @Test func classesAreOrderedByWidth() {
        #expect(WindowWidth.allCases == WindowWidth.allCases.sorted())
        #expect(WindowWidth.compact < .extraLarge)
    }
}
