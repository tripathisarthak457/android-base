package com.base.app.core.designsystem.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.base.app.core.designsystem.component.container.AppDivider
import com.base.app.core.designsystem.component.feedback.AppBadgedBox
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppSurface
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.BarTreatment

/**
 * One destination in the bottom bar or the navigation rail. [selectedIcon] is separate from [icon]
 * so a tab can switch from an outline to a filled glyph when active — a second, non-colour signal
 * for the selected state.
 */
@Immutable
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0,
)

/** The persistent bottom bar, drawn the way the design style says — see [BarTreatment]. */
@Composable
fun AppBottomBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (AppTheme.style.bar) {
        BarTreatment.Floating -> FloatingBar(items, selectedIndex, onItemSelected, modifier)
        else -> EdgeBar(items, selectedIndex, onItemSelected, modifier)
    }
}

object AppBottomBarDefaults {

    private val FloatingHeight = 64.dp
    private val FloatingMargin = 12.dp

    /** How much of the bottom of the window the bar covers, navigation bar included. */
    @Composable
    fun occupiedHeight(): Dp {
        val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        return when (AppTheme.style.bar) {
            BarTreatment.Floating -> FloatingHeight + FloatingMargin * 2 + navigationBar
            else -> AppTheme.sizes.bottomBarHeight + AppTheme.sizes.borderWidth + navigationBar
        }
    }

    internal val floatingHeight: Dp get() = FloatingHeight
    internal val floatingMargin: Dp get() = FloatingMargin
}

@Composable
private fun EdgeBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier,
) {
    val colors = AppTheme.colors
    val treatment = AppTheme.style.bar

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface),
    ) {
        if (treatment == BarTreatment.Chunky) {
            AppDivider(thickness = AppTheme.sizes.borderWidth, color = colors.contentPrimary)
        } else {
            AppDivider(color = colors.divider)
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(AppTheme.sizes.bottomBarHeight),
        ) {
            if (treatment == BarTreatment.Minimal && items.isNotEmpty()) {
                SlidingRule(
                    selectedIndex = selectedIndex,
                    itemWidth = maxWidth / items.size,
                )
            }

            ItemRow(items, selectedIndex, onItemSelected, Modifier.fillMaxHeight())
        }
    }
}

@Composable
private fun FloatingBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier,
) {
    AppSurface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(
                horizontal = AppTheme.spacing.gutter + AppBottomBarDefaults.floatingMargin,
                vertical = AppBottomBarDefaults.floatingMargin,
            )
            .fillMaxWidth()
            .height(AppBottomBarDefaults.floatingHeight),
        shape = AppTheme.shapes.pill,
        elevation = AppTheme.elevation.overlay,
    ) {
        BoxWithConstraints(modifier = Modifier.padding(6.dp)) {
            if (items.isNotEmpty()) {
                SlidingPill(selectedIndex = selectedIndex, itemWidth = maxWidth / items.size)
            }
            ItemRow(items, selectedIndex, onItemSelected, Modifier.fillMaxHeight())
        }
    }
}

@Composable
private fun SlidingPill(selectedIndex: Int, itemWidth: Dp) {
    val x by animateDpAsState(
        targetValue = itemWidth * selectedIndex,
        animationSpec = AppTheme.motion.sheet(),
        label = "barPill",
    )
    Box(
        modifier = Modifier
            .offset(x = x)
            .width(itemWidth)
            .fillMaxHeight()
            .background(AppTheme.colors.accentSubtle, AppTheme.shapes.pill),
    )
}

@Composable
private fun SlidingRule(selectedIndex: Int, itemWidth: Dp) {
    val x by animateDpAsState(
        targetValue = itemWidth * selectedIndex,
        animationSpec = AppTheme.motion.sheet(),
        label = "barRule",
    )
    Box(
        modifier = Modifier
            .offset(x = x + itemWidth / 2 - RuleWidth / 2)
            .width(RuleWidth)
            .height(2.dp)
            .background(AppTheme.colors.contentPrimary),
    )
}

private val RuleWidth = 20.dp

@Composable
private fun ItemRow(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            BottomBarItem(
                item = item,
                selected = index == selectedIndex,
                onClick = { onItemSelected(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun BottomBarItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val treatment = AppTheme.style.bar
    val chunky = treatment == BarTreatment.Chunky

    val tint by animateColorAsState(
        targetValue = when {
            chunky && selected -> colors.onAccent
            selected && treatment == BarTreatment.Minimal -> colors.contentPrimary
            selected -> colors.accent
            else -> colors.contentTertiary
        },
        animationSpec = tween(AppTheme.motion.medium, easing = AppTheme.motion.standard),
        label = "navTint",
    )
    val labelTint by animateColorAsState(
        targetValue = when {
            selected && (chunky || treatment == BarTreatment.Minimal) -> colors.contentPrimary
            selected -> colors.accent
            else -> colors.contentTertiary
        },
        animationSpec = tween(AppTheme.motion.medium, easing = AppTheme.motion.standard),
        label = "navLabelTint",
    )
    val pill by animateColorAsState(
        targetValue = if (chunky && selected) colors.accent else Color.Transparent,
        animationSpec = tween(AppTheme.motion.quick),
        label = "navPill",
    )
    // Only the edge bars lift the icon. On the floating bar the sliding pill already says which
    // tab is selected, and a second movement inside it reads as a wobble.
    val iconScale by animateFloatAsState(
        targetValue = if (selected && treatment != BarTreatment.Floating) 1.06f else 1f,
        animationSpec = AppTheme.motion.press(),
        label = "navScale",
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        AppBadgedBox(count = item.badgeCount) {
            Box(
                modifier = Modifier
                    .background(pill, AppTheme.shapes.pill)
                    .padding(horizontal = if (chunky) 14.dp else 0.dp, vertical = if (chunky) 3.dp else 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    imageVector = if (selected) item.selectedIcon else item.icon,
                    contentDescription = null,
                    tint = tint,
                    size = AppTheme.sizes.iconLarge,
                    modifier = Modifier.scale(iconScale),
                )
            }
        }
        val uppercase = AppTheme.style.uppercaseLabels
        AppText(
            text = if (uppercase) item.label.uppercase() else item.label,
            style = if (uppercase) {
                AppTheme.typography.labelSmall.copy(letterSpacing = 0.1.em)
            } else {
                AppTheme.typography.labelSmall
            },
            color = labelTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
