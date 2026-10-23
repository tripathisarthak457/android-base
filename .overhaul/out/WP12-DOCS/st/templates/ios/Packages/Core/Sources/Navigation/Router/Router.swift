import Observation
import SwiftUI

/// The navigation state of one tab: the screens pushed on its stack, and the item its list-detail
/// root shows.
///
/// Each tab owns one, so switching tabs keeps every tab where it was. Views bind `path` to the
/// tab's `NavigationStack` and `selection` to a split view's list. A store whose own work decides
/// where to go next, such as opening what it has just saved, receives the router at init.
@MainActor
@Observable
public final class Router {
    /// The screens above the tab's root, in order.
    public var path = NavigationPath()

    /// The id of the item a list-detail root shows in its detail column, in the form a deep link
    /// carries it. Nil shows the list on a phone and the placeholder beside it on an iPad.
    public var selection: String?

    public init() {}

    /// How many screens are pushed above the root.
    public var depth: Int {
        path.count
    }

    public func push<Route: Hashable>(_ route: Route) {
        path.append(route)
    }

    public func pop() {
        guard !path.isEmpty else { return }
        path.removeLast()
    }

    /// Back to the tab's first screen, with nothing selected: what tapping the selected tab does.
    public func popToRoot() {
        path = NavigationPath()
        selection = nil
    }
}
