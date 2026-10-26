package com.example.finalafter.crui.core.devtools.page

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

/**
 * A tab of the inspector that another module contributes with `@Provides @IntoSet`, and
 * optionally a line under the badge's label that opens it.
 *
 * For tooling that only some builds carry: that module is a dependency of those builds alone, so
 * every other build holds neither the tab nor its code, rather than both behind a runtime check.
 *
 * @property key names the tab in the panel's saved state.
 * @property content composed only while this tab is the open one, so nothing it draws costs the
 *   app a frame while the panel is closed.
 * @property badgeContent composed under the badge's label whenever the badge shows. Its argument
 *   opens the panel on this tab.
 */
class DevToolsPage(
    val key: String,
    @get:StringRes val title: Int,
    val content: @Composable () -> Unit,
    val badgeContent: (@Composable (openPage: () -> Unit) -> Unit)? = null,
)
