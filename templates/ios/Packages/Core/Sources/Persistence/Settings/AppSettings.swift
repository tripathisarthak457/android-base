/// The person's settings, read in one go.
public struct AppSettings: Equatable, Sendable {
    public var appearance: Appearance
    public var hapticsEnabled: Bool
    public var hasCompletedOnboarding: Bool

    public init(appearance: Appearance, hapticsEnabled: Bool, hasCompletedOnboarding: Bool) {
        self.appearance = appearance
        self.hapticsEnabled = hapticsEnabled
        self.hasCompletedOnboarding = hasCompletedOnboarding
    }
}
