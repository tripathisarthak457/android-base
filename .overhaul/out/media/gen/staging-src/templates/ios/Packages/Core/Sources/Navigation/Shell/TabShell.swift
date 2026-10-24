import SwiftUI

/// The app's tabs: a tab bar on a phone, and on iPadOS 18 or later a tab bar that becomes a sidebar.
///
/// Every tab is kept alive while it is not selected, so its stack, scroll position and state are
/// where they were when it comes back. Tapping the tab that is already selected calls `onReselect`,
/// where the app pops that tab to its root. More than five tabs on a phone are gathered under
/// More by the system.
public struct TabShell<Item: ShellTab, Content: View>: View {
    @Binding private var selection: Item
    private let tabs: [Item]
    private let onReselect: (Item) -> Void
    private let content: (Item) -> Content

    public init(
        selection: Binding<Item>,
        tabs: [Item],
        onReselect: @escaping (Item) -> Void,
        @ViewBuilder content: @escaping (Item) -> Content
    ) {
        _selection = selection
        self.tabs = tabs
        self.onReselect = onReselect
        self.content = content
    }

    public var body: some View {
        if #available(iOS 18, *) {
            AdaptableTabs(selection: reselecting, tabs: tabs, content: content)
        } else {
            TabView(selection: reselecting) {
                ForEach(tabs) { tab in
                    content(tab)
                        .tabItem { TabLabel(tab: tab) }
                        .tag(tab)
                }
            }
        }
    }

    // The tab bar sets the selection again when the selected tab is tapped; that is the only way
    // SwiftUI reports a reselect.
    private var reselecting: Binding<Item> {
        Binding {
            selection
        } set: { tab in
            if tab == selection {
                onReselect(tab)
            } else {
                selection = tab
            }
        }
    }
}

@available(iOS 18, *)
private struct AdaptableTabs<Item: ShellTab, Content: View>: View {
    @Binding var selection: Item
    let tabs: [Item]
    let content: (Item) -> Content

    @AppStorage("shell.tabCustomization") private var customization: TabViewCustomization

    var body: some View {
        TabView(selection: $selection) {
            ForEach(tabs) { tab in
                Tab(value: tab) {
                    content(tab)
                } label: {
                    TabLabel(tab: tab)
                }
                .customizationID(tab.id)
            }
        }
        .tabViewStyle(.sidebarAdaptable)
        .tabViewCustomization($customization)
        .modifier(MinimizesTabBarOnScroll())
    }
}

private struct TabLabel<Item: ShellTab>: View {
    let tab: Item

    var body: some View {
        Label {
            Text(tab.title)
        } icon: {
            Image(systemName: tab.systemImage)
        }
    }
}

/// On iOS 26 the tab bar shrinks while content scrolls down and returns when it scrolls back, so
/// the content gets the room. Earlier releases keep the bar as it is.
private struct MinimizesTabBarOnScroll: ViewModifier {
    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26, *) {
            content.tabBarMinimizeBehavior(.onScrollDown)
        } else {
            content
        }
    }
}
