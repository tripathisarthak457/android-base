import SwiftUI

/// What a screen shows instead of content: nothing yet, nothing found, or nothing at all, with the
/// actions that lead out of it. It is `ContentUnavailableView`, styled by the theme.
///
/// For a failed load, use `AppFailureView`, which words each `AppError` and offers a retry.
public struct AppStateView<Actions: View>: View {
    private let title: Text
    private let systemImage: String
    private let message: Text?
    private let actions: Actions

    @Environment(\.appTheme) private var theme

    public init(_ title: Text, systemImage: String, message: Text? = nil, @ViewBuilder actions: () -> Actions) {
        self.title = title
        self.systemImage = systemImage
        self.message = message
        self.actions = actions()
    }

    public var body: some View {
        ContentUnavailableView {
            Label {
                title
            } icon: {
                Image(systemName: systemImage)
                    .foregroundStyle(theme.colors.contentTertiary)
            }
        } description: {
            if let message {
                message
            }
        } actions: {
            actions
        }
    }
}

extension AppStateView where Actions == EmptyView {
    public init(_ title: Text, systemImage: String, message: Text? = nil) {
        self.init(title, systemImage: systemImage, message: message) {
            EmptyView()
        }
    }
}
