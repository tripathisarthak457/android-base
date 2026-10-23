package io.example.fitcnet.core.designsystem.component.sheet

import io.example.fitcnet.core.designsystem.foundation.interaction.AppHaptic
import io.example.fitcnet.core.designsystem.theme.AppSpring
import io.example.fitcnet.core.designsystem.theme.AppSprings
import kotlinx.coroutines.CoroutineScope

/**
 * What an [AppSheetState] needs from the sheet it is drawn by while that sheet is composed: the
 * theme's springs, the caller's callbacks and the decisions about closing. The state reads it from
 * its motion code, outside composition, so every member reads the latest value.
 */
internal interface SheetHost {
    val springs: AppSprings

    /** Presets arrive on the dialog spring; every other sheet on the sheet spring. */
    val presentSpring: AppSpring

    val crossFade: Boolean

    /** The person may close the sheet: dismissible, and no guard is registered. */
    val dismissAllowed: Boolean

    /** The caller's `visible` is false: a show() from code does nothing. */
    val callerHides: Boolean

    /** Laid out, slot providers composed and measured, and the owner done arriving. */
    val readyToPresent: Boolean

    val density: Float

    /** The coroutine scope of the sheet's layout, for motion started from callbacks. */
    val scope: CoroutineScope

    fun haptic(haptic: AppHaptic)

    /** hide() was called while the caller still shows the sheet: its onDismissRequest runs once. */
    fun onHiddenByCode()

    /** The person settled the sheet on a different detent. */
    fun onDetentChanged(detent: SheetDetent)

    /** The sheet has left the screen, however it was closed. */
    fun onGone()

    /**
     * The call site left composition while the sheet showed: it leaves the stack and stops moving its
     * host in that frame, before its layout is composed out. Its caller is gone, so nothing is called.
     */
    fun leftComposition()

    /** The person tried to close a sheet that may not close: the innermost guard hears of it. */
    fun attempt(source: SheetDismissSource)

    /** The person closed the sheet: it leaves now, from where it is, at [velocity]. */
    fun closeByPerson(source: SheetDismissSource, velocity: Float)
}
