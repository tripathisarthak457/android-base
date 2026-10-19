import SwiftUI

@main
struct BaseAppApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                // Compose draws under the status bar, the home indicator and the keyboard, and
                // applies those insets itself, as the Android app does edge to edge.
                .ignoresSafeArea()
        }
    }
}
