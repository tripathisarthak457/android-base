import SwiftUI

/// The motion style's springs, built once per theme rather than per animation.
///
/// Each is the token's (response, damping) pair as a mass-1 spring, the same curve the Android
/// template runs. SwiftUI springs retarget from their current velocity when interrupted, so every
/// animation that uses one can be reversed mid-flight without a jump.
public nonisolated struct AppSprings: Sendable {
    /// A finger coming down on a control.
    public let press: Animation
    /// A finger lifting; carries the style's bounce.
    public let release: Animation
    /// Anything that moves a whole screen. Never bounces.
    public let screen: Animation
    /// Sheets and their detents.
    public let sheet: Animation
    /// Rows moving, inserting and removing, and sections expanding.
    public let layout: Animation
    /// A badge or a success mark arriving.
    public let emphasis: Animation

    init(motion: DesignTokens.MotionStyle) {
        press = .spring(motion.press.spring)
        release = .spring(motion.release.spring)
        screen = .spring(motion.screen.spring)
        sheet = .spring(motion.sheet.spring)
        layout = .spring(motion.layout.spring)
        emphasis = .spring(motion.emphasis.spring)
    }
}
