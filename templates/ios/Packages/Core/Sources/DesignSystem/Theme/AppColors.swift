import SwiftUI

/// The colour roles of the design style, resolved for one appearance.
///
/// Use roles, never raw colours: a role keeps its contrast in dark appearance and under every
/// design style, and the brand colours the project was generated with arrive through `accent`.
public nonisolated struct AppColors: Sendable {
    public let background: Color
    /// The background of an inset-grouped list, behind its sections.
    public let backgroundGrouped: Color
    public let surface: Color
    /// A section of an inset-grouped list.
    public let surfaceGrouped: Color
    /// Anything that floats above the page: sheets, menus, toasts. Lighter than `surface` in dark
    /// appearance, as iOS lifts elevated surfaces.
    public let surfaceElevated: Color
    public let surfaceInverse: Color
    /// Tracks, wells and other quiet fills.
    public let fill: Color
    /// Hairlines between rows and around outlined containers.
    public let separator: Color
    public let contentPrimary: Color
    public let contentSecondary: Color
    public let contentTertiary: Color
    public let contentDisabled: Color
    /// Text and icons on `surfaceInverse`.
    public let contentInverse: Color
    /// A pressed row.
    public let highlight: Color
    public let scrim: Color
    public let focusRing: Color
    public let shadow: Color
    /// A placeholder's bones, as `appRedacted(_:)` draws them.
    public let skeleton: Color
    /// The middle of the shimmer band where it crosses a bone.
    public let skeletonHighlight: Color
    public let accent: Color
    public let accentPressed: Color
    /// A tinted background in the accent's hue.
    public let accentSubtle: Color
    /// Text and icons on `accent`.
    public let onAccent: Color
    /// The accent as text or an icon on a surface, darkened or lightened until it reads at 4.5:1.
    public let accentContent: Color
    public let success: AppStatusColors
    public let warning: AppStatusColors
    public let danger: AppStatusColors
    public let info: AppStatusColors
    public let neutral: AppStatusColors

    init(roles: DesignTokens.Roles) {
        background = roles.background
        backgroundGrouped = roles.backgroundGrouped
        surface = roles.surface
        surfaceGrouped = roles.surfaceGrouped
        surfaceElevated = roles.surfaceElevated
        surfaceInverse = roles.surfaceInverse
        fill = roles.fill
        separator = roles.separator
        contentPrimary = roles.contentPrimary
        contentSecondary = roles.contentSecondary
        contentTertiary = roles.contentTertiary
        contentDisabled = roles.contentDisabled
        contentInverse = roles.contentInverse
        highlight = roles.highlight
        scrim = roles.scrim
        focusRing = roles.focusRing
        shadow = roles.shadow
        skeleton = roles.skeleton
        skeletonHighlight = roles.skeletonHighlight
        accent = roles.accent
        accentPressed = roles.accentPressed
        accentSubtle = roles.accentSubtle
        onAccent = roles.onAccent
        accentContent = roles.accentContent
        success = AppStatusColors(roles.success)
        warning = AppStatusColors(roles.warning)
        danger = AppStatusColors(roles.danger)
        info = AppStatusColors(roles.info)
        neutral = AppStatusColors(roles.neutral)
    }
}
