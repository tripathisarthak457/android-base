package com.base.app.core.navigation

import androidx.compose.runtime.mutableStateListOf
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.common.navigation.FullScreenKey
import kotlinx.serialization.Serializable

@Serializable
internal data object FeedTab : AppNavKey

@Serializable
internal data object ProfileTab : AppNavKey

@Serializable
internal data class Post(val id: Int) : AppNavKey

@Serializable
internal data object Compose : AppNavKey

@Serializable
internal data object Player : FullScreenKey

/** A two-tab shell: Feed (whose graph owns the posts) and Profile, with Compose presented. */
internal fun shell(
    selected: Int = 0,
    stacks: List<List<AppNavKey>> = listOf(listOf(FeedTab), listOf(ProfileTab)),
    presentedKeys: Set<AppNavKey> = setOf(Compose),
): ShellState = ShellState(
    rootKeys = stacks.map { it.first() },
    tabs = stacks.map { stack -> AppBackStack(mutableStateListOf<AppNavKey>().apply { addAll(stack) }) },
    presented = AppBackStack(mutableStateListOf(), floor = 0),
    initialTab = selected,
    generation = 1,
    isPresented = { it in presentedKeys },
    sameGraph = { a, b -> setOf(a, b).all { it == FeedTab || it is Post } },
)
