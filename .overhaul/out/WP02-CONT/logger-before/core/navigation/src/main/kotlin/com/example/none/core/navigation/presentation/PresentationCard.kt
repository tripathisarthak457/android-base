package com.example.none.core.navigation.presentation

import androidx.compose.runtime.Stable
import androidx.navigationevent.NavigationEvent
import com.example.none.core.common.navigation.AppNavKey
import com.example.none.core.navigation.AppBackStack
import com.example.none.core.navigation.back.BackCardHost
import com.example.none.core.navigation.back.BackCardState
import com.example.none.core.navigation.back.commitPop
import com.example.none.core.navigation.transition.ScreenMotion
import kotlinx.coroutines.CoroutineScope

/**
 * The back card over a cover: the cover shrinks over the shell, which waits under the card's scrim
 * with its tab bar, as Gmail's card shrinks over the screen it returns to.
 *
 * Only the one cover over the shell becomes a card. `NavDisplay` keeps no presented screen composed
 * under the one on top, so a card over a second cover would show the shell and then cut to the
 * cover beneath on commit; that Back, and a sheet's, pops with its slide instead.
 *
 * @param shrinks whether a presented key is drawn as a cover rather than by a sheet scene.
 */
@Stable
internal class PresentationCard(
    private val presented: AppBackStack,
    private val shrinks: (AppNavKey) -> Boolean,
) : BackCardHost {

    override val card = BackCardState()

    var motion = ScreenMotion()

    override fun canStartCard(): Boolean =
        !card.active && !presented.gate.moving && presented.size == 1 && shrinks(presented.current)

    override fun startCard(event: NavigationEvent, scope: CoroutineScope) {
        card.start(event, subject = presented.current, beneath = emptySet(), midPush = false, scope = scope)
    }

    override suspend fun cancelCard() {
        card.cancel(midPush = false, fullWidth = true)
    }

    override suspend fun commitCard(pop: (Int) -> Int, releasedNanos: Long, scope: CoroutineScope) {
        card.commitPop(presented.gate, motion.fraction, pop, releasedNanos, scope)
    }
}
