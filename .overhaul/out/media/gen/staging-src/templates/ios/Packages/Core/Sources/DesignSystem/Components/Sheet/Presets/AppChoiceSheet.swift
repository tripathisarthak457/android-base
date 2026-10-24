import SwiftUI

extension View {
    /// Presents `choices` in a sheet with the current one checked: a language, a unit, a plan.
    /// Choosing one sets `selection` and closes the sheet.
    ///
    /// The sheet opens at half height and grows to full; from six choices a search field filters
    /// them by `label` and `supportingText`, ignoring case and accents. `title` names the sheet for
    /// VoiceOver.
    public func appChoiceSheet<Choice: Hashable>(
        isPresented: Binding<Bool>,
        title: Text,
        choices: [Choice],
        selection: Binding<Choice?>,
        label: @escaping (Choice) -> String,
        supportingText: ((Choice) -> String?)? = nil
    ) -> some View {
        appSheet(isPresented: isPresented, options: AppSheetOptions(detents: [.medium, .large])) {
            AppChoiceList(
                title: title,
                choices: choices,
                selection: selection,
                label: label,
                supportingText: supportingText,
                close: { isPresented.wrappedValue = false }
            )
        }
    }
}

struct AppChoiceList<Choice: Hashable>: View {
    let title: Text
    let choices: [Choice]
    @Binding var selection: Choice?
    let label: (Choice) -> String
    let supportingText: ((Choice) -> String?)?
    let close: () -> Void

    @State private var query = ""

    @Environment(\.appTheme) private var theme

    /// Six, as in the Android template: a shorter list is read at a glance.
    static var searchesFrom: Int { 6 }

    var body: some View {
        let shown = Self.choices(choices, matching: query, label: label, supportingText: supportingText)
        withSearchField(list(shown))
            .overlay {
                if shown.isEmpty && !choices.isEmpty {
                    ContentUnavailableView.search(text: query)
                }
            }
            .appSheetHeader(title)
    }

    private func list(_ shown: [Choice]) -> some View {
        List {
            AppSection {
                ForEach(shown, id: \.self) { choice in
                    row(choice)
                }
            }
        }
        .appGroupedList()
    }

    @ViewBuilder
    private func withSearchField(_ content: some View) -> some View {
        if choices.count >= Self.searchesFrom {
            content.searchable(text: $query)
        } else {
            content
        }
    }

    private func row(_ choice: Choice) -> some View {
        let isSelected = choice == selection
        let subtitle = supportingText?(choice)
        return Button {
            selection = choice
            close()
        } label: {
            AppRow(Text(verbatim: label(choice)), subtitle: subtitle.map { Text(verbatim: $0) }) {
                if isSelected {
                    Image(systemName: "checkmark")
                        .foregroundStyle(theme.colors.accentContent)
                        .accessibilityHidden(true)
                }
            }
        }
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    /// The choices whose label or supporting text contains `query`, ignoring case and accents and
    /// following the person's locale; all of them while the query is blank.
    static func choices(
        _ choices: [Choice],
        matching query: String,
        label: (Choice) -> String,
        supportingText: ((Choice) -> String?)?
    ) -> [Choice] {
        let query = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return choices }
        return choices.filter { choice in
            label(choice).localizedStandardContains(query)
                || supportingText?(choice)?.localizedStandardContains(query) == true
        }
    }
}

#Preview("Choice sheet") {
    @Previewable @State var isPresented = true
    @Previewable @State var language: String? = "Português"
    Button {
        isPresented = true
    } label: {
        Text(verbatim: language ?? "Choose a language")
    }
    .appChoiceSheet(
        isPresented: $isPresented,
        title: Text(verbatim: "Language"),
        choices: ["Deutsch", "English", "Español", "Français", "हिन्दी", "Português"],
        selection: $language,
        label: { $0 }
    )
    .appTheme()
}
