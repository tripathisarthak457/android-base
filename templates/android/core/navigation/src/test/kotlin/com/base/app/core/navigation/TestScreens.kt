package com.base.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.base.app.core.common.navigation.AppNavKey
// <opt:ui_components>
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.icon.AppIcons
// </opt:ui_components>

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

// <opt:ui_components>
/** Feed and Profile tabs. The labels only need to be some string the test can resolve. */
internal val testTabs = listOf(
    ShellTab(FeedTab, R.string.designsystem_search, AppIcons.Home),
    ShellTab(ProfileTab, R.string.designsystem_refresh, AppIcons.User),
)
// </opt:ui_components>

internal val testSerialization = NavKeySerialization(testGraphs())
