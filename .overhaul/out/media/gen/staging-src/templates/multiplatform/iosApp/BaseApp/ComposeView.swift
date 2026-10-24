import Shared
import SwiftUI
import UIKit

/// The shared Compose UI, which draws every screen of the app.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(environment: .current)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

extension AppEnvironment {
    /// The values `Configuration/<name>.xcconfig` set for this build, through `Info.plist`.
    static var current: AppEnvironment {
        let info = Bundle.main.infoDictionary ?? [:]
        #if DEBUG
        let isDebug = true
        #else
        let isDebug = false
        #endif
        return AppEnvironment(
            apiBaseUrl: info["AppApiBaseUrl"] as? String ?? "",
            name: info["AppEnvironment"] as? String ?? "",
            isDebug: isDebug,
            versionName: info["CFBundleShortVersionString"] as? String ?? ""
        )
    }
}
