import SwiftUI

/// One tab of the app's shell, as the app declares its tabs.
public nonisolated protocol ShellTab: Hashable, Identifiable, Sendable where ID == String {
    /// What the tab bar, the sidebar and VoiceOver call the tab.
    var title: LocalizedStringResource { get }

    /// The SF Symbol for the tab. Each tab needs its own, or two tabs look like one.
    var systemImage: String { get }
}
