/// How a design style's type sounds. Each voice changes the weights only; sizes follow Dynamic Type.
public nonisolated enum TypeVoice: String, CaseIterable, Sendable {
    /// The scale as Apple's text styles define it.
    case neutral = "Neutral"

    /// Headlines and titles a weight heavier.
    case bold = "Bold"

    /// Large, heavy titles over a light body, for reading apps.
    case editorial = "Editorial"

    /// Heavier throughout, for playful apps.
    case rounded = "Rounded"
}
