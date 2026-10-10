import DesignSystem
import Navigation
import Persistence
import SwiftUI
// <opt:onboarding>
import OnboardingFeature
// </opt:onboarding>

/// A window's content, under the theme and appearance the person chose: the walkthrough on the
/// first run, then the tabs.
struct AppWindow: View {
    @State private var model: AppModel

    init(services: AppServices) {
        _model = State(initialValue: AppModel(services: services))
    }

    var body: some View {
        AppGate(model: model)
            .appTheme(hapticsEnabled: model.settings.hapticsEnabled)
            .preferredColorScheme(model.settings.appearance.colorScheme)
            .toastHost(model.toasts)
            .measuresWindowWidth()
            // The smallest window the layouts are designed for, on iPad and Mac.
            .frame(minWidth: 320, minHeight: 400)
            .focusedSceneValue(\.appModel, model)
            .task { await model.followSettings() }
            .onOpenURL { model.open($0) }
    }
}

private struct AppGate: View {
    let model: AppModel

    // <opt:onboarding>
    @Environment(\.appTheme) private var theme

    // </opt:onboarding>
    var body: some View {
        // <opt:onboarding>
        ZStack {
            if model.settings.hasCompletedOnboarding {
                tabs
                    .transition(.move(edge: .trailing))
            } else {
                OnboardingView(settings: model.services.settings)
                    .transition(.move(edge: .leading))
            }
        }
        .animation(theme.springs.screen, value: model.settings.hasCompletedOnboarding)
        // </opt:onboarding>
        // <opt-else:onboarding>
        //~ tabs
        // </opt-else:onboarding>
    }

    private var tabs: some View {
        @Bindable var model = model
        return TabShell(selection: $model.selection, tabs: AppTab.allCases, onReselect: model.reselect) { tab in
            tab.root(in: model)
                .environment(model.router(for: tab))
        }
    }
}

private extension Appearance {
    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}
