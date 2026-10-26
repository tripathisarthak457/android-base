package com.example.finalafter.crui.core.designsystem.foundation.interaction

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onLongClick

/**
 * Actions a container offers for what it holds: the context menu around a row, the actions a swipe
 * reveals behind it. A screen reader stops on the control's merged node, so the outermost
 * `pressable` inside claims them and offers them as its own; on the container they would be a
 * second, nested stop that says nothing. When nothing inside claims them, the container merges its
 * content into one node that offers them ([containerActionsSemantics]).
 */
internal class ContainerActions(
    val menuLabel: String?,
    val openMenu: (() -> Unit)?,
    val actions: List<CustomAccessibilityAction>,
) {
    private var owner: Any? = null
    private var onClaimChanged: (() -> Unit)? = null

    val isClaimed: Boolean
        get() = owner != null

    fun isOwnedBy(node: Any): Boolean = owner === node

    /** Takes the actions for [node] unless a control further out already has them. */
    fun claim(node: Any) {
        if (owner != null) return
        owner = node
        onClaimChanged?.invoke()
    }

    fun release(node: Any) {
        if (owner !== node) return
        owner = null
        onClaimChanged?.invoke()
    }

    internal fun watchClaims(onChange: (() -> Unit)?) {
        onClaimChanged = onChange
    }
}

internal val LocalContainerActions = compositionLocalOf<ContainerActions?> { null }

/** The container's own offer of [actions], for content with no control to carry them. */
internal fun Modifier.containerActionsSemantics(actions: ContainerActions?): Modifier =
    if (actions == null) this else this then ContainerActionsElement(actions)

private class ContainerActionsElement(val actions: ContainerActions) : ModifierNodeElement<ContainerActionsNode>() {
    override fun create() = ContainerActionsNode(actions)

    override fun update(node: ContainerActionsNode) = node.update(actions)

    override fun InspectorInfo.inspectableProperties() {
        name = "containerActionsSemantics"
    }

    override fun equals(other: Any?): Boolean = other is ContainerActionsElement && other.actions === actions

    override fun hashCode(): Int = System.identityHashCode(actions)
}

private class ContainerActionsNode(private var actions: ContainerActions) : Modifier.Node(), SemanticsModifierNode {

    override val shouldMergeDescendantSemantics: Boolean
        get() = !actions.isClaimed

    override fun onAttach() = actions.watchClaims(::invalidateSemantics)

    override fun onDetach() = actions.watchClaims(null)

    fun update(actions: ContainerActions) {
        if (actions === this.actions) return
        this.actions.watchClaims(null)
        this.actions = actions
        if (isAttached) actions.watchClaims(::invalidateSemantics)
        invalidateSemantics()
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (actions.isClaimed) return
        actions.openMenu?.let { open ->
            onLongClick(label = actions.menuLabel) {
                open()
                true
            }
        }
        if (actions.actions.isNotEmpty()) customActions = actions.actions
    }
}
