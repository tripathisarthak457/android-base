import DesignSystem
import Persistence
import SwiftUI

/// The first-run walkthrough. Finishing or skipping it is written to the settings store, and the
/// app's root, which observes the store, moves on to the tabs.
public struct OnboardingView: View {
    @State private var store: OnboardingStore

    @Environment(\.appTheme) private var theme

    public init(settings: SettingsRepository) {
        _store = State(initialValue: OnboardingStore(settings: settings, pageCount: OnboardingPage.allCases.count))
    }

    public var body: some View {
        VStack(spacing: AppSpacing.xl) {
            TabView(selection: page) {
                ForEach(OnboardingPage.allCases) { page in
                    OnboardingPageView(page: page)
                        .tag(page.rawValue)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .always))
            .indexViewStyle(.page(backgroundDisplayMode: .interactive))
            .appHaptic(.selection, trigger: store.state.page)

            Button {
                store.send(.next)
            } label: {
                Group {
                    if store.state.isLastPage {
                        Text("Get started", bundle: .module)
                    } else {
                        Text("Next", bundle: .module)
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.app(.filled, size: .large))
            .padding(.horizontal, AppSpacing.gutter)
            .padding(.bottom, AppSpacing.lg)
        }
        .frame(maxWidth: 560)
        .frame(maxWidth: .infinity)
        .background(theme.colors.background)
        .overlay(alignment: .topTrailing) {
            Button {
                store.send(.skip)
            } label: {
                Text("Skip", bundle: .module)
                    .font(theme.typography.body)
                    .foregroundStyle(theme.colors.accentContent)
                    .padding(AppSpacing.lg)
            }
            .buttonStyle(.dimPress)
            .keyboardShortcut(.cancelAction)
        }
    }

    private var page: Binding<Int> {
        Binding {
            store.state.page
        } set: { page in
            store.send(.pageChanged(page))
        }
    }
}

private struct OnboardingPageView: View {
    let page: OnboardingPage

    @Environment(\.appTheme) private var theme
    @ScaledMetric(relativeTo: .largeTitle) private var symbolSize: CGFloat = 72

    var body: some View {
        VStack(spacing: AppSpacing.lg) {
            Image(systemName: page.systemImage)
                .font(.system(size: symbolSize))
                .foregroundStyle(theme.colors.accent)
                .symbolRenderingMode(.hierarchical)
                .accessibilityHidden(true)
            page.title
                .font(theme.typography.title1)
                .foregroundStyle(theme.colors.contentPrimary)
                .multilineTextAlignment(.center)
            page.message
                .font(theme.typography.body)
                .foregroundStyle(theme.colors.contentSecondary)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, AppSpacing.xxl)
        .accessibilityElement(children: .combine)
    }
}

#Preview("iPhone") {
    OnboardingView(settings: SettingsRepository(hapticsDefault: true))
        .appTheme()
}

#Preview("iPad", traits: .fixedLayout(width: 1194, height: 834)) {
    OnboardingView(settings: SettingsRepository(hapticsDefault: true))
        .appTheme(designStyle: .social)
}
