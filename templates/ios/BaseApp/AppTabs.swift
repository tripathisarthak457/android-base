// <generated:shell-tabs>
import Navigation
import SampleFeature
import SettingsFeature
import SwiftUI

/// The app's tabs, in the order the tab bar shows them.
nonisolated enum AppTab: String, CaseIterable, ShellTab {
    case sample
    case settings

    var id: String {
        rawValue
    }

    var title: LocalizedStringResource {
        switch self {
        case .sample: "Home"
        case .settings: "Settings"
        }
    }

    var systemImage: String {
        switch self {
        case .sample: "house"
        case .settings: "gearshape"
        }
    }

    /// The tab's first screen, built from the window's services.
    @MainActor
    @ViewBuilder
    func root(in model: AppModel) -> some View {
        switch self {
        case .sample:
            SampleRootView(
                client: model.services.apiClient,
                modelContainer: model.services.modelContainer,
                monitor: model.services.networkMonitor,
                toasts: model.toasts
            )
        case .settings:
            SettingsRootView(settings: model.services.settings)
        }
    }
}
// </generated:shell-tabs>
