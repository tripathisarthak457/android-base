import AppCore
import DesignSystem
import Foundation
// <opt:network>
import Networking
// </opt:network>
import OSLog
import Persistence
// <opt:sample&database>
import SampleFeature
import SwiftData
// </opt:sample&database>

/// The services every window and every feature share, built once when the app starts.
///
/// This is the composition root: the one place that knows which implementation stands behind each
/// dependency. Features receive what they need at init and never reach for a global.
@MainActor
final class AppServices {
    let settings: SettingsRepository
    // <opt:network>
    let apiClient: APIClient
    let networkMonitor = NetworkMonitor()
    // </opt:network>
    // <opt:sample&database>
    let modelContainer: ModelContainer
    // </opt:sample&database>

    init(bundle: Bundle = .main, defaults: UserDefaults = .standard) {
        settings = SettingsRepository(defaults: defaults, hapticsDefault: AppTheme.defaultHapticsEnabled)
        // <opt:network>
        apiClient = APIClient(baseURL: Self.apiBaseURL(in: bundle))
        // </opt:network>
        // <opt:sample&database>
        modelContainer = Self.makeModelContainer()
        // </opt:sample&database>
    }
    // <opt:network>

    private static func apiBaseURL(in bundle: Bundle) -> URL {
        guard let text = bundle.object(forInfoDictionaryKey: "APIBaseURL") as? String, let url = URL(string: text) else {
            preconditionFailure("APIBaseURL is missing from Info.plist. It is set from API_BASE_URL in Config/*.xcconfig.")
        }
        return url
    }
    // </opt:network>
    // <opt:sample&database>

    private static func makeModelContainer() -> ModelContainer {
        let models = SampleRootView.persistentModels
        do {
            return try ModelContainerFactory.make(for: models)
        } catch {
            // A store that cannot open, such as after a model change shipped without a migration,
            // must not stop the app from starting. Nothing is kept this session, and the fault is
            // in the log for the crash and diagnostics tools to pick up.
            Log.logger(category: "App").fault("The database did not open: \(String(describing: error), privacy: .public)")
            do {
                return try ModelContainerFactory.make(for: models, inMemory: true)
            } catch {
                fatalError("Not even an in-memory database opened: \(error)")
            }
        }
    }
    // </opt:sample&database>
}
