package com.base.app.core.designsystem.component.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A rounded surface for one item: a list row that carries more than a line, a summary. With
 * [onClick] the whole card is the target and shrinks under the finger.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(AppTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val pressable = if (onClick != null) Modifier.pressable(onClick, PressFeedback.Scale) else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(pressable)
            .clip(AppTheme.style.sectionShape)
            .background(AppTheme.colors.surfaceGrouped)
            .padding(contentPadding),
        content = content,
    )
}
