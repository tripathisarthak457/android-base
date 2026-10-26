package com.example.cmp.core.navigation.shell

import androidx.compose.runtime.mutableStateListOf
import com.example.cmp.core.common.navigation.AppNavKey
import com.example.cmp.core.common.navigation.FullScreenKey
import com.example.cmp.core.navigation.AppBackStack
import com.example.cmp.core.navigation.TabStacks
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
    tabStacks = TabStacks(roots = stacks.map { it.first() }, stacks = stacks, selected = selected),
    presented = AppBackStack(mutableStateListOf(), floor = 0),
    generation = 1,
    isPresented = { it in presentedKeys },
    sameGraph = { a, b -> setOf(a, b).all { it == FeedTab || it is Post } },
)
