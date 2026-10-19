import DesignSystem
import SwiftUI
import UIKit

/// Opens the app's page in the Settings app, where iOS lists every language the app ships and keeps
/// the choice for this app alone. iOS restarts the app in the new language, so the app stores
/// nothing itself.
struct LanguageRow: View {
    @Environment(\.openURL) private var openURL

    var body: some View {
        Button {
            if let url = URL(string: UIApplication.openSettingsURLString) {
                openURL(url)
            }
        } label: {
            AppRow(
                Text("App language", tableName: "Language", bundle: .module),
                subtitle: Text("Choose it in the Settings app.", tableName: "Language", bundle: .module),
                systemImage: "globe"
            ) {
                Image(systemName: "arrow.up.forward.app")
                    .accessibilityHidden(true)
            }
        }
    }
}
