/// How a design style draws a text field.
public nonisolated enum FieldTreatment: String, CaseIterable, Sendable {
    /// A box with an outline that thickens and takes the accent on focus.
    case outlined = "Outlined"

    /// A tinted box with no outline until it has focus or an error.
    case filled = "Filled"

    /// A line under the text, as on paper.
    case underlined = "Underlined"
}
