package com.base.app.core.navigation.shell

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import com.base.app.core.designsystem.component.container.AppListItem
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.feedback.AppBadgedBox
import com.base.app.core.designsystem.component.navigation.AppTopBar
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.foundation.layout.ReportScrollEdge
import com.base.app.core.designsystem.foundation.layout.chromeContentPadding
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.R
import com.base.app.core.navigation.ShellTab

/**
 * The More tab's list: the tabs the chrome has no room for, from [from] on, each opening that tab
 * in place of the list. They stay tabs, with their own stacks, so a window wide enough for a rail
 * or a sidebar shows them as tabs again with nothing lost.
 */
@Composable
internal fun MoreList(tabs: List<ShellTab>, labels: List<String>, from: Int, onSelect: (Int) -> Unit) {
    val chevron = if (LocalLayoutDirection.current == LayoutDirection.Rtl) AppIcons.ChevronLeft else AppIcons.ChevronRight
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        topBar = { AppTopBar(title = stringResource(R.string.navigation_more)) },
    ) {
        val listState = rememberLazyListState()
        ReportScrollEdge(listState)
        LazyColumn(state = listState, contentPadding = chromeContentPadding()) {
            // Keys are saved with the list's state, so they must fit in a Bundle: the tab's index.
            items(count = tabs.size - from, key = { from + it }) { offset ->
                val index = from + offset
                val tab = tabs[index]
                AppListItem(
                    title = labels[index],
                    modifier = Modifier.testTag("more_$index"),
                    onClick = { onSelect(index) },
                    leading = {
                        AppBadgedBox(count = tab.badgeCount) {
                            AppIcon(imageVector = tab.icon, contentDescription = null, tint = AppTheme.colors.accent)
                        }
                    },
                    trailing = {
                        AppIcon(imageVector = chevron, contentDescription = null, tint = AppTheme.colors.contentTertiary)
                    },
                )
            }
        }
    }
}
