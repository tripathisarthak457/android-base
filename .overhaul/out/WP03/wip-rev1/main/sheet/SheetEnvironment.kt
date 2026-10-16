package com.base.app.core.designsystem.component.sheet

import androidx.compose.ui.unit.Density
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.theme.AppSprings
import kotlinx.coroutines.CoroutineScope

/**
 * What a sheet's layout node lends its [AppSheetState] while attached. The state reads springs and
 * density from here rather than from fields written in composition, so a sheet that outlives a
 * theme change moves with the new theme's springs.
 */
internal interface SheetEnvironment {
    val springs: AppSprings
    val density: Density

    /** Reduce motion, or a cross-fade transition: the sheet appears and leaves by fading in place. */
    val crossFade: Boolean

    /** The node's scope: animations the layout starts (a detent that moved, a park) run here. */
    val scope: CoroutineScope

    fun perform(haptic: AppHaptic)
}

/** The composable's side of closing a sheet, which the state consults when the person lets go. */
internal interface SheetBehaviour {
    /** False while `dismissible` is false or a [SheetDismissGuard] is up. */
    val dismissAllowed: Boolean

    /** The person closed the sheet: runs `onDismissRequest`, once. */
    fun dismissed(source: SheetDismissSource)

    /** The person tried to close a sheet that may not close: the innermost guard hears it. */
    fun attempted(source: SheetDismissSource)

    /** The person settled the sheet on a different detent. */
    fun detentChanged(detent: SheetDetent)

    object None : SheetBehaviour {
        override val dismissAllowed: Boolean get() = true

        override fun dismissed(source: SheetDismissSource) = Unit

        override fun attempted(source: SheetDismissSource) = Unit

        override fun detentChanged(detent: SheetDetent) = Unit
    }
}
