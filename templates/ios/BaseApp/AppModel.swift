import DesignSystem
import Foundation
import Navigation
import Observation
import Persistence

/// One window's state: the selected tab, one router per tab, its toasts, and the person's settings
/// as the window last heard them.
///
/// Each window has its own, so two iPad windows can show different tabs and stacks.
@MainActor
@Observable
final class AppModel {
    var selection: AppTab
    private(set) var settings: AppSettings

    let services: AppServices
    let toasts = ToastCenter()
    private let routers: [AppTab: Router]

    init(services: AppServices) {
        self.services = services
        settings = services.settings.current
        selection = AppTab.allCases[0]
        routers = Dictionary(uniqueKeysWithValues: AppTab.allCases.map { ($0, Router()) })
    }

    func router(for tab: AppTab) -> Router {
        guard let router = routers[tab] else {
            preconditionFailure("Every tab gets a router when the window opens.")
        }
        return router
    }

    /// Tapping the selected tab again takes it back to its first screen.
    func reselect(_ tab: AppTab) {
        router(for: tab).popToRoot()
    }

    /// Shows what a link names: its tab, at its root, with its item selected.
    func open(_ url: URL) {
        guard let link = DeepLink(url: url, scheme: Self.urlScheme), let tab = AppTab(rawValue: link.tab) else {
            return
        }
        selection = tab
        let router = router(for: tab)
        router.popToRoot()
        router.selection = link.item
    }

    /// Follows the settings store for as long as the window is open.
    func followSettings() async {
        for await settings in services.settings.changes() {
            self.settings = settings
        }
    }

    private static let urlScheme = "baseapp"
}
