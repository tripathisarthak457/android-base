package com.example.finalafter.crnoui3.core.common

import com.example.finalafter.crnoui3.core.common.result.AppError

/** How a screen's primary content is currently doing. */
sealed interface LoadState {

    /** Nothing requested yet. */
    data object Idle : LoadState

    /** First load, nothing to show. Render a shape-matched skeleton. */
    data object Loading : LoadState

    /** A reload with content already on screen: pull-to-refresh, or a silent revalidation. */
    data object Refreshing : LoadState

    /** Loaded, and the content is authoritative. */
    data object Success : LoadState

    /** Loaded successfully, and the result was empty. */
    data object Empty : LoadState

    /** Failed. The screen turns [error] into words, because what to say depends on where it is. */
    data class Error(val error: AppError) : LoadState
}

/** True when content should be rendered, including while it refreshes underneath. */
val LoadState.hasContent: Boolean
    get() = this is LoadState.Success || this is LoadState.Refreshing

val LoadState.isRefreshing: Boolean get() = this is LoadState.Refreshing
