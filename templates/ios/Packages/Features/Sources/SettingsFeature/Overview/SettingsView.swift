import DesignSystem
import Persistence
import SwiftUI

/// Appearance, haptics and the app's version, in an inset-grouped list.
struct SettingsView: View {
    let store: SettingsStore

    var body: some View {
        List {
            AppSection(Text("Appearance", bundle: .module)) {
                Picker(selection: appearance) {
                    Text("System", bundle: .module).tag(Appearance.system)
                    Text("Light", bundle: .module).tag(Appearance.light)
                    Text("Dark", bundle: .module).tag(Appearance.dark)
                } label: {
                    Text("Theme", bundle: .module)
                }
                .pickerStyle(.segmented)
                .appHaptic(.selection, trigger: store.state.appearance)
            } footer: {
                Text("System follows your device setting.", bundle: .module)
            }
            AppSection {
                Toggle(isOn: haptics) {
                    Text("Haptic feedback", bundle: .module)
                }
            } footer: {
                Text("A small vibration when a control responds.", bundle: .module)
            }
            // <opt:language>
            AppSection(Text("Language", tableName: "Language", bundle: .module)) {
                LanguageRow()
            }
            // </opt:language>
            AppSection(Text("About", bundle: .module)) {
                AppRow(Text("Version", bundle: .module)) {
                    Text(store.state.version)
                }
            }
        }
        .appGroupedList()
        .navigationTitle(Text("Settings", bundle: .module))
    }

    private var appearance: Binding<Appearance> {
        Binding {
            store.state.appearance
        } set: { appearance in
            store.send(.appearanceChanged(appearance))
        }
    }

    private var haptics: Binding<Bool> {
        Binding {
            store.state.hapticsEnabled
        } set: { enabled in
            store.send(.hapticsChanged(enabled))
        }
    }
}

#Preview("iPhone") {
    NavigationStack {
        SettingsView(store: SettingsStore(settings: SettingsRepository(hapticsDefault: true), version: "1.0.0 (1)"))
    }
    .appTheme()
}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {
    NavigationStack {
        SettingsView(store: SettingsStore(settings: SettingsRepository(hapticsDefault: true), version: "1.0.0 (1)"))
    }
    .appTheme(designStyle: .playful)
}
