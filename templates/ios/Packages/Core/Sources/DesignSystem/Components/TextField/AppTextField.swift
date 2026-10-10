import SwiftUI

/// A labelled single-line text field drawn with the design style's field treatment, with room for
/// an error under it.
///
/// The text belongs to the store's state, and edits go back through its events:
///
///     AppTextField(
///         Text("Email", bundle: .module),
///         text: Binding(get: { store.state.email }, set: { store.send(.emailChanged($0)) }),
///         error: store.state.emailError.map { Text($0) }
///     )
public struct AppTextField: View {
    private let label: Text
    @Binding private var text: String
    private let prompt: Text?
    private let error: Text?

    @FocusState private var isFocused: Bool
    @Environment(\.appTheme) private var theme

    public init(_ label: Text, text: Binding<String>, prompt: Text? = nil, error: Text? = nil) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.error = error
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            label
                .font(theme.typography.footnote)
                .foregroundStyle(theme.colors.contentSecondary)
                .textCase(theme.shapes.uppercaseLabels ? .uppercase : nil)
                .accessibilityHidden(true)
            TextField(text: $text, prompt: prompt) {
                label
            }
            .font(theme.typography.body)
            .foregroundStyle(theme.colors.contentPrimary)
            .focused($isFocused)
            .padding(.horizontal, theme.shapes.field == .underlined ? 0 : AppSpacing.md)
            .frame(minHeight: AppSizes.fieldHeight)
            .background {
                FieldBackground(treatment: theme.shapes.field, outline: outline, emphasised: emphasised)
            }
            .animation(.easeOut(duration: DesignTokens.Motion.fadeShort), value: emphasised)
            if let error {
                error
                    .font(theme.typography.footnote)
                    .foregroundStyle(theme.colors.danger.content)
            }
        }
    }

    private var emphasised: Bool {
        isFocused || error != nil
    }

    private var outline: Color {
        if error != nil {
            return theme.colors.danger.base
        }
        return isFocused ? theme.colors.accent : theme.colors.separator
    }
}

private struct FieldBackground: View {
    let treatment: FieldTreatment
    let outline: Color
    let emphasised: Bool

    @Environment(\.appTheme) private var theme

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: theme.shapes.radiusSm, style: .continuous)
        let width = emphasised ? theme.shapes.borderWidthStrong : theme.shapes.borderWidth
        switch treatment {
        case .outlined:
            shape.strokeBorder(outline, lineWidth: width)
        case .filled:
            shape
                .fill(theme.colors.fill)
                .overlay {
                    if emphasised {
                        shape.strokeBorder(outline, lineWidth: width)
                    }
                }
        case .underlined:
            Rectangle()
                .fill(outline)
                .frame(height: width)
                .frame(maxHeight: .infinity, alignment: .bottom)
        }
    }
}

#Preview("Text fields") {
    @Previewable @State var name = ""
    VStack(spacing: AppSpacing.lg) {
        AppTextField(Text(verbatim: "Name"), text: $name, prompt: Text(verbatim: "Jane Appleseed"))
        AppTextField(
            Text(verbatim: "Email"),
            text: .constant("jane@"),
            error: Text(verbatim: "Enter a full email address.")
        )
    }
    .padding()
    .appTheme()
}
