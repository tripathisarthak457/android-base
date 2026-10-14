import SwiftUI

/// One action of `appActionSheet`: a label, an optional role and what it does.
public struct AppSheetAction {
    let label: Text
    let role: ButtonRole?
    let action: () -> Void

    /// - Parameter role: `.destructive` for an action that deletes or discards, which the system
    ///   draws in red. Leave Cancel out: the system adds it where the platform shows one.
    public init(_ label: Text, role: ButtonRole? = nil, action: @escaping () -> Void) {
        self.label = label
        self.role = role
        self.action = action
    }
}
