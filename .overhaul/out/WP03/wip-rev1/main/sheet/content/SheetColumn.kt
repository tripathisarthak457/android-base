package com.base.app.core.designsystem.component.sheet.content

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.sheet.LocalSheetSurface

/** The enclosing sheet's `contentPadding`; zero outside a sheet. */
@Composable
@ReadOnlyComposable
fun sheetContentPadding(): PaddingValues = LocalSheetSurface.current?.scope?.contentPadding ?: NoPadding

/**
 * A scrolling column with the sheet's content padding applied inside the scroll, for a form or a
 * settings group in a sheet. The sheet adds no scroller of its own: a lazy list brings its own and
 * takes `contentPadding` directly.
 */
@Composable
fun SheetColumn(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = sheetContentPadding(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(state)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

private val NoPadding = PaddingValues(0.dp)
