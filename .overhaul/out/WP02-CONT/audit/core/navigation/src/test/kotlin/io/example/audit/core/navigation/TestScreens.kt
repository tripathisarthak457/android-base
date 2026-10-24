package io.example.audit.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.example.audit.core.common.navigation.AppNavKey
import io.example.audit.core.designsystem.R
import io.example.audit.core.designsystem.icon.AppIcons

/** A screen that is nothing but its key, tagged with it. */
@Composable
internal fun Screen(key: AppNavKey) {
    Box(Modifier.fillMaxSize().testTag(key.toString())) { BasicText(key.toString()) }
}

internal fun testRegistry(screen: @Composable (AppNavKey) -> Unit = { Screen(it) }): NavRegistry = NavRegistry(
    setOf(
        navGraph {
            entry<FeedTab> { screen(it) }
            entry<Post> { screen(it) }
            entry<Player> { screen(it) }
        },
        navGraph {
            entry<ProfileTab> { screen(it) }
            entry<Compose>(presentation = Presentation.Cover) { screen(it) }
        },
    ),
)

/** Feed and Profile tabs. The labels only need to be some string the test can resolve. */
internal val testTabs = listOf(
    ShellTab(FeedTab, R.string.designsystem_search, AppIcons.Home),
    ShellTab(ProfileTab, R.string.designsystem_refresh, AppIcons.User),
)

internal val testSerialization = NavKeySerialization(
    setOf(
        navKeys {
            subclass(FeedTab::class, FeedTab.serializer())
            subclass(ProfileTab::class, ProfileTab.serializer())
            subclass(Post::class, Post.serializer())
            subclass(Compose::class, Compose.serializer())
            subclass(Player::class, Player.serializer())
        },
    ),
)
