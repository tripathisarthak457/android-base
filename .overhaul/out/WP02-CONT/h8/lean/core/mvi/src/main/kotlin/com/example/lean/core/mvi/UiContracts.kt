package com.example.lean.core.mvi

/**
 * The three halves of a screen's contract, as markers so a ViewModel's type parameters read as
 * intent. [UiEffect] is what happens once and is not state (navigate, open the dialler): put it in
 * state and a navigation fires a second time on rotation.
 */
interface UiState

interface UiEvent

interface UiEffect
