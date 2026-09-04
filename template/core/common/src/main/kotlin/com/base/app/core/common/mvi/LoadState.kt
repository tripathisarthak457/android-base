package com.base.app.core.common.mvi

import com.base.app.core.common.util.UiText

/**
 * How a screen's primary content is currently doing.
 *
 * This replaces the `isLoading: Boolean` + `error: String?` pair that most UiStates start with.
 * That pair has four representable combinations and only three meaningful ones — "loading *and*
 * errored" is nonsense the type permits, and in practice it renders as a spinner sitting on top
 * of an error message. A sealed state cannot express it.
 *
 * The distinction that earns its keep day to day is [Loading] versus [Refreshing]: the first has
 * nothing to show and gets a skeleton, the second already has content on screen and must not
 * replace it with one. Collapsing them is why pull-to-refresh so often blanks the list it was
 * asked to refresh.
 */
sealed interface LoadState {

    /** Nothing requested yet. */
    data object Idle : LoadState

    /** First load, nothing to show. Render a shape-matched skeleton. */
    data object Loading : LoadState

    /** A reload with content already on screen — pull-to-refresh, or a silent revalidation. */
    data object Refreshing : LoadState

    /** Loaded, and the content is authoritative. */
    data object Success : LoadState

    /**
     * Loaded successfully, and the result was empty.
     *
     * Distinct from [Success] with zero rows because an empty state carries its own copy and its
     * own call to action, which a generic success cannot supply.
     */
    data object Empty : LoadState

    /**
     * Failed. [message] is display-ready; [isOffline] separates "no connection" from "the server
     * said no", because those get different copy and different actions — retry-when-reconnected
     * versus retry-now.
     */
    data class Error(
        val message: UiText,
        val isOffline: Boolean = false,
        val code: Int? = null,
    ) : LoadState
}

/** True while a first load is in flight with nothing to show yet. */
val LoadState.isInitialLoading: Boolean get() = this is LoadState.Loading

/** True when content should be rendered — including while it is refreshing underneath. */
val LoadState.hasContent: Boolean
    get() = this is LoadState.Success || this is LoadState.Refreshing

val LoadState.isRefreshing: Boolean get() = this is LoadState.Refreshing

val LoadState.isTerminal: Boolean
    get() = this is LoadState.Success || this is LoadState.Empty || this is LoadState.Error
