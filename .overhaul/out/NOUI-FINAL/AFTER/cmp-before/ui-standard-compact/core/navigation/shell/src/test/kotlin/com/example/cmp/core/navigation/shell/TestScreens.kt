package com.example.cmp.core.navigation.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.cmp.core.common.navigation.AppNavKey
import com.example.cmp.core.designsystem.R
import com.example.cmp.core.designsystem.icon.AppIcons
import com.example.cmp.core.navigation.NavGraphEntry
import com.example.cmp.core.navigation.NavKeySerialization
import com.example.cmp.core.navigation.NavRegistry
import com.example.cmp.core.navigation.Presentation
import com.example.cmp.core.navigation.navGraph

/** A screen that is nothing but its key, tagged with it. */
@Composable
internal fun Screen(key: AppNavKey) {
    Box(Modifier.fillMaxSize().testTag(key.toString())) { BasicText(key.toString()) }
}

/** Two features' graphs: Feed's (the feed, its posts and the player) and Profile's, with Compose presented. */
internal fun testGraphs(screen: @Composable (AppNavKey) -> Unit = { Screen(it) }): Set<NavGraphEntry> = setOf(
    navGraph {
        entry<FeedTab> { screen(it) }
        entry<Post> { screen(it) }
        entry<Player> { screen(it) }
    },
    navGraph {
        entry<ProfileTab> { screen(it) }
        entry<Compose>(presentation = Presentation.Cover) { screen(it) }
    },
)

internal fun testRegistry(screen: @Composable (AppNavKey) -> Unit = { Screen(it) }): NavRegistry = NavRegistry(testGraphs(screen))

/** Feed and Profile tabs. The labels only need to be some string the test can resolve. */
internal val testTabs = listOf(
    ShellTab(FeedTab, R.string.designsystem_search, AppIcons.Home),
    ShellTab(ProfileTab, R.string.designsystem_refresh, AppIcons.User),
)

internal val testSerialization = NavKeySerialization(testGraphs())
