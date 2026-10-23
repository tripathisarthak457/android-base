package com.base.app.core.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.theme.AppTheme

/** One tab of an [AppTabBar]. [selectedIcon] is the filled glyph shown while the tab is selected. */
@Immutable
data class TabItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

/**
 * A docked tab bar: full width, a hairline above, an icon and a label per tab. Tapping the
 * selected tab again calls [onReselect], which a shell turns into "back to the root" or "scroll to
 * the top", as iOS does.
 */
@Composable
fun AppTabBar(
    items: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onReselect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .drawBehind { drawLine(colors.separator, Offset.Zero, Offset(size.width, 0f), strokeWidth = 1f) }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(AppTheme.sizes.tabBarHeight),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selected
            val tint = if (isSelected) colors.accentContent else colors.contentSecondary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics { this.selected = isSelected }
                    .pressable(
                        onClick = { if (isSelected) onReselect(index) else onSelect(index) },
                        feedback = PressFeedback.None,
                        role = Role.Tab,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AppIcon(
                    if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = null,
                    tint = tint,
                    size = AppTheme.sizes.iconLarge,
                )
                AppText(
                    text = item.label,
                    style = AppTheme.typography.caption2,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
