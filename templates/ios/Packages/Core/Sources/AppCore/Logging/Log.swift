import Foundation
import OSLog

/// Loggers for the app's subsystems.
///
/// Every logger shares the app's bundle identifier as its subsystem, so Console.app and
/// `log stream --predicate 'subsystem == "<bundle id>"'` show the whole app in one place.
public enum Log {
    public static func logger(category: String) -> Logger {
        Logger(subsystem: Bundle.main.bundleIdentifier ?? "app", category: category)
    }
}
