import DesignSystem
import Foundation
import Navigation
import Persistence
import SwiftUI

/// The Settings tab.
public struct SettingsRootView: View {
    @Environment(Router.self) private var router
    @State private var store: SettingsStore

    public init(settings: SettingsRepository) {
        _store = State(initialValue: SettingsStore(settings: settings, version: Bundle.main.versionDescription))
    }

    public var body: some View {
        @Bindable var router = router
        NavigationStack(path: $router.path) {
            SettingsView(store: store)
        }
    }
}

private extension Bundle {
    /// "1.2.0 (42)": the marketing version and the build.
    var versionDescription: String {
        let version = object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "?"
        let build = object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "?"
        return "\(version) (\(build))"
    }
}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {
    SettingsRootView(settings: SettingsRepository(hapticsDefault: true))
        .environment(Router())
        .appTheme()
}
