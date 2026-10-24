package io.example.audit.core.designsystem.component.sheet.action

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/** One choice in an [AppActionSheet]. Remember the list it is in: a list rebuilt on every recomposition defeats skipping. */
@Immutable
class SheetAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    /** Draws a checkmark and announces the row as selected. */
    val selected: Boolean = false,
) {
    fun copy(
        label: String = this.label,
        onClick: () -> Unit = this.onClick,
        icon: ImageVector? = this.icon,
        destructive: Boolean = this.destructive,
        enabled: Boolean = this.enabled,
        selected: Boolean = this.selected,
    ): SheetAction = SheetAction(label, onClick, icon, destructive, enabled, selected)

    override fun equals(other: Any?): Boolean = other is SheetAction &&
        label == other.label && onClick == other.onClick && icon == other.icon &&
        destructive == other.destructive && enabled == other.enabled && selected == other.selected

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + onClick.hashCode()
        result = 31 * result + (icon?.hashCode() ?: 0)
        result = 31 * result + destructive.hashCode()
        result = 31 * result + enabled.hashCode()
        return 31 * result + selected.hashCode()
    }

    override fun toString(): String =
        "SheetAction(label=$label, destructive=$destructive, enabled=$enabled, selected=$selected)"
}
