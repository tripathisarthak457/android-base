package com.base.app.core.common.mvi

/**
 * The three halves of a screen's contract. Marker interfaces, so a `MviViewModel`'s type
 * parameters read as intent rather than as `<A, B, C>`.
 *
 * The split is the whole point of the pattern:
 *
 * - [UiState] is everything the screen renders, and it is complete. Rendering must never need a
 *   value that is not in here — no reading a repository from a composable, no second source of
 *   truth to fall out of sync.
 * - [UiEvent] is everything the user or the system can do to the screen. The composable's only
 *   outward channel.
 * - [UiEffect] is what happens *once* and is not state: navigate, dismiss the keyboard, open the
 *   dialler. Putting these in state is how a navigation fires a second time on rotation.
 */
interface UiState

interface UiEvent

interface UiEffect

/** For a screen whose contract genuinely has no one-shot effects. */
object NoEffect : UiEffect
