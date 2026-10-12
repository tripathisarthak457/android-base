import DesignSystem
import Foundation
// <opt:network>
import Networking
// </opt:network>
import Persistence
// <opt:sample&database>
import AppCore
import OSLog
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
    /// Started by the first screen that follows connectivity, so the app's tests start no path monitor.
    lazy var networkMonitor = NetworkMonitor()
    // </opt:network>
    // <opt:sample&database>
    let modelContainer: ModelContainer
    // </opt:sample&database>

    // <opt:sample&database>
    /// - Parameter keepsDataInMemory: for tests, which must neither keep data on disk nor read the app's.
    init(defaults: UserDefaults = .standard, keepsDataInMemory: Bool = false) {
    // </opt:sample&database>
    // <opt-else:sample&database>
    //~ init(defaults: UserDefaults = .standard) {
    // </opt-else:sample&database>
        settings = SettingsRepository(defaults: defaults, hapticsDefault: AppTheme.defaultHapticsEnabled)
        // <opt:network>
        apiClient = APIClient(baseURL: Self.apiBaseURL())
        // </opt:network>
        // <opt:sample&database>
        modelContainer = Self.makeModelContainer(inMemory: keepsDataInMemory)
        // </opt:sample&database>
    }
    // <opt:network>

    private static func apiBaseURL() -> URL {
        guard let text = Bundle.main.object(forInfoDictionaryKey: "APIBaseURL") as? String, let url = URL(string: text) else {
            preconditionFailure("APIBaseURL is missing from Info.plist. It is set from API_BASE_URL in Config/*.xcconfig.")
        }
        return url
    }
    // </opt:network>
    // <opt:sample&database>

    private static func makeModelContainer(inMemory: Bool) -> ModelContainer {
        let models = SampleRootView.persistentModels
        do {
            return try ModelContainerFactory.make(for: models, inMemory: inMemory)
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
