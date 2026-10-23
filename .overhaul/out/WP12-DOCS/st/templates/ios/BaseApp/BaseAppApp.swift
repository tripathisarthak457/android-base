import SwiftUI

@main
struct BaseAppApp: App {
    @State private var services = AppServices()

    var body: some Scene {
        WindowGroup {
            AppWindow(services: services)
        }
        .windowResizability(.contentMinSize)
        .commands {
            AppCommands()
        }
    }
}
