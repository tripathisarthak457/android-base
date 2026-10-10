/// How a design style draws a card's container.
public nonisolated enum CardTreatment: String, CaseIterable, Sendable {
    /// A hairline outline and no shadow.
    case outlined = "Outlined"

    /// No outline, a soft shadow in light appearance and a lifted surface in dark.
    case raised = "Raised"

    /// A strong outline with a hard shadow offset down and to the trailing side.
    case offset = "Offset"
}
