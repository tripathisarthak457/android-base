package com.example.cmp.core.navigation.back

import com.example.cmp.core.navigation.AppBackStack
import com.example.cmp.core.navigation.NavCommand
import com.example.cmp.core.navigation.NavigationHostApi

/**
 * What to do with a navigation command, given the host it targets.
 *
 * - The destination already on top is not pushed again: a double-tapped row opens one screen.
 * - A push arriving while a push is still moving is dropped, as `UINavigationController` drops it:
 *   two rows tapped within one transition open the first.
 * - Anything else arriving mid-transition (a push after a pop, Up from an effect, a deep link) waits
 *   for the transition to settle: retargeting a running transition is what makes screens snap.
 */
@NavigationHostApi
fun guard(command: NavCommand, host: AppBackStack): Verdict {
    if (command is NavCommand.Navigate && command.singleTop && command.popUpTo == null &&
        host.entries.lastOrNull() == command.key
    ) {
        return Verdict.Drop
    }
    if (!host.gate.moving) return Verdict.Apply
    val pushDuringPush = command is NavCommand.Navigate && host.gate.lastChange == StackChange.Push
    return if (pushDuringPush) Verdict.Drop else Verdict.Wait
}

/** What [guard] decided: apply the command now, drop it, or apply it once the host stops moving. */
@NavigationHostApi
enum class Verdict { Apply, Drop, Wait }

/** Applies [command] to this stack if [guard] lets it, and tells the stack's gate what changed. */
@NavigationHostApi
fun AppBackStack.submit(command: NavCommand): Verdict {
    val verdict = guard(command, this)
    if (verdict == Verdict.Apply) {
        apply(command)
        gate.changed(change(command))
    }
    return verdict
}
