package com.base.app.core.common.mvi

import com.base.app.core.common.util.UiText

/** How a screen's primary content is currently doing. */
sealed interface LoadState {

    /** Nothing requested yet. */
    data object Idle : LoadState

    /** First load, nothing to show. Render a shape-matched skeleton. */
    data object Loading : LoadState

    /** A reload with content already on screen — pull-to-refresh, or a silent revalidation. */
    data object Refreshing : LoadState

    /** Loaded, and the content is authoritative. */
    data object Success : LoadState

    /** Loaded successfully, and the result was empty. */
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
