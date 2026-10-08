package com.base.app.core.designsystem.component.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.container.AppVerticalDivider
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.BarTreatment

/**
 * The bottom bar's destinations stood on their side, for windows of Medium width and up: tablets,
 * unfolded foldables, phones held sideways and desktop windows. Drawn with the same items and the same
 * design-style treatment as [AppBottomBar], so switching between them changes nothing but the
 * layout.
 */
@Composable
fun AppNavigationRail(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val treatment = AppTheme.style.bar

    Row(modifier = modifier.fillMaxHeight().background(colors.surface)) {
        Box(
            // Insets first: a camera cutout on this side widens the rail rather than squeezing its
            // labels into what is left of it.
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(RailInsets)
                .width(AppNavigationRailDefaults.Width)
                .padding(vertical = AppTheme.spacing.lg),
        ) {
            if (items.isNotEmpty()) {
                when (treatment) {
                    BarTreatment.Floating -> VerticalPill(selectedIndex)
                    BarTreatment.Minimal -> LeadingRule(selectedIndex)
                    else -> Unit
                }
            }
            Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                items.forEachIndexed { index, item ->
                    BottomBarItem(
                        item = item,
                        selected = index == selectedIndex,
                        onClick = { onItemSelected(index) },
                        modifier = Modifier.fillMaxWidth().height(ItemHeight),
                    )
                }
            }
        }
        if (treatment == BarTreatment.Chunky) {
            AppVerticalDivider(color = colors.contentPrimary, thickness = AppTheme.sizes.borderWidth)
        } else {
            AppVerticalDivider(color = colors.divider)
        }
    }
}

object AppNavigationRailDefaults {

    val Width: Dp = 88.dp
}

@Composable
private fun VerticalPill(selectedIndex: Int) {
    val y by animateDpAsState(
        targetValue = ItemHeight * selectedIndex,
        animationSpec = AppTheme.motion.sheet(),
        label = "railPill",
    )
    Box(
        modifier = Modifier
            .offset(y = y)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .fillMaxWidth()
            .height(ItemHeight - 8.dp)
            .background(AppTheme.colors.accentSubtle, AppTheme.shapes.pill),
    )
}

@Composable
private fun LeadingRule(selectedIndex: Int) {
    val y by animateDpAsState(
        targetValue = ItemHeight * selectedIndex + ItemHeight / 2 - RuleLength / 2,
        animationSpec = AppTheme.motion.sheet(),
        label = "railRule",
    )
    Box(
        modifier = Modifier
            .offset(y = y)
            .width(2.dp)
            .height(RuleLength)
            .background(AppTheme.colors.contentPrimary),
    )
}

private val ItemHeight = 68.dp
private val RailInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
private val RuleLength = 24.dp
