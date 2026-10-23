import SwiftUI

/// Keyboard shortcuts for the tabs, ⌘1 to ⌘9, in the iPad's menu bar and its ⌘ overlay. They act
/// on the window in front.
struct AppCommands: Commands {
    @FocusedValue(\.appModel) private var model

    var body: some Commands {
        CommandMenu("Navigate") {
            ForEach(Array(AppTab.allCases.prefix(9).enumerated()), id: \.element) { index, tab in
                Button {
                    model?.selection = tab
                } label: {
                    Text(tab.title)
                }
                .keyboardShortcut(KeyEquivalent(Character(String(index + 1))), modifiers: .command)
                .disabled(model == nil)
            }
        }
    }
}

extension FocusedValues {
    /// The model of the window in front, for menu commands.
    nonisolated var appModel: AppModel? {
        get { self[AppModelKey.self] }
        set { self[AppModelKey.self] = newValue }
    }
}

private nonisolated struct AppModelKey: FocusedValueKey {
    typealias Value = AppModel
}
