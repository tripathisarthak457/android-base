import SwiftUI

/// The dismiss guards in one sheet's content, so the sheet's close button asks what a swipe asks.
///
/// Each guard registers what it does on an attempt to close the sheet and replaces it on every
/// update, so the close button always reaches the guard's latest closure. Nothing here is observed:
/// it changes as often as the guarded view updates, and no view draws from it.
final class AppSheetGuards {
    private var attempts: [ObjectIdentifier: () -> Void] = [:]

    /// Sets what `owner` does when someone tries to close the sheet; nil while it does not guard.
    func setAttempt(_ onAttempt: (() -> Void)?, for owner: AnyObject) {
        attempts[ObjectIdentifier(owner)] = onAttempt
    }

    /// Reports an attempt to close the sheet to every guard, as a swipe does, and returns whether
    /// any guard took it. Without one the caller closes the sheet.
    func attemptDismissal() -> Bool {
        let current = Array(attempts.values)
        for attempt in current {
            attempt()
        }
        return !current.isEmpty
    }
}

extension EnvironmentValues {
    /// The guards of the sheet around this view, or nil outside a sheet the app presented.
    nonisolated var appSheetGuards: AppSheetGuards? {
        get { self[AppSheetGuardsKey.self] }
        set { self[AppSheetGuardsKey.self] = newValue }
    }
}

private nonisolated struct AppSheetGuardsKey: EnvironmentKey {
    static let defaultValue: AppSheetGuards? = nil
}
