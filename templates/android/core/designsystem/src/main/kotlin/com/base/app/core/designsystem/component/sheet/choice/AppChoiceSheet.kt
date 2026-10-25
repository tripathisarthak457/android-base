package com.base.app.core.designsystem.component.sheet.choice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.input.AppSearchField
import com.base.app.core.designsystem.component.list.AppChoiceRow
import com.base.app.core.designsystem.component.sheet.AppSheetDefaults
import com.base.app.core.designsystem.component.sheet.AppSheetProperties
import com.base.app.core.designsystem.component.sheet.AppSheetScope
import com.base.app.core.designsystem.component.sheet.HostedSheet
import com.base.app.core.designsystem.component.sheet.SheetDetent
import com.base.app.core.designsystem.component.sheet.SheetModality
import com.base.app.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.base.app.core.designsystem.component.sheet.content.sheetFooterItem
import com.base.app.core.designsystem.component.sheet.rememberAppSheetState
import com.base.app.core.designsystem.foundation.content.rememberCurrentLocale
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.tokens.DesignTokens

/**
 * One choice from a list, the current one checked: a language, a unit, a plan. Rows have the radio
 * role and may carry [supportingText] and be disabled. A search field appears from
 * [AppChoiceSheetDefaults.SearchFrom] choices; it matches the label and the supporting text, ignoring
 * case and accents. [onSelectNone] adds a row labelled [noneLabel] that selects nothing. Choosing
 * calls onSelect, then onDismissRequest. [key] must return a value a Bundle can hold; by default rows
 * are keyed by position.
 */
@Composable
public fun <T : Any> AppChoiceSheet(
    visible: Boolean,
    choices: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    onDismissRequest: () -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    title: String? = null,
    supportingText: ((T) -> String?)? = null,
    enabled: (T) -> Boolean = { true },
    onSelectNone: (() -> Unit)? = null,
    noneLabel: String = stringResource(R.string.designsystem_none),
    key: ((T) -> Any)? = null,
) {
    val state = rememberAppSheetState(ChoiceDetents)
    HostedSheet(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        state = state,
        modality = SheetModality.Modal,
        dismissible = true,
        properties = DefaultProperties,
        style = AppSheetDefaults.style(),
        paneTitle = title,
        onDetentChange = null,
        onHidden = null,
        grabber = AppSheetDefaults.Grabber,
        header = title?.let { text -> { AppSheetHeader(title = text) } },
        footer = null,
        content = {
            ChoiceList(choices, selected, onSelect, label, supportingText, enabled, onSelectNone, noneLabel, key)
        },
        asDialog = true,
    )
}

public object AppChoiceSheetDefaults {
    /** From this many choices the sheet offers a search field: past a screenful, scanning is slower than typing. */
    public const val SearchFrom: Int = DesignTokens.Sheet.Choice.searchFrom

    /** The most of Large's height the sheet opens at before the list scrolls or the person pulls it up. */
    public const val MaxFitFraction: Float = DesignTokens.Sheet.Choice.maxFitFraction
}

@Composable
private fun <T : Any> AppSheetScope.ChoiceList(
    choices: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    supportingText: ((T) -> String?)?,
    enabled: (T) -> Boolean,
    onSelectNone: (() -> Unit)?,
    noneLabel: String,
    key: ((T) -> Any)?,
) {
    val query = rememberTextFieldState()
    val locale = rememberCurrentLocale()
    val searching = choices.size >= AppChoiceSheetDefaults.SearchFrom
    val text = query.text.toString()
    val shown = remember(choices, text, locale, label, supportingText) {
        if (!searching || text.isBlank()) {
            choices
        } else {
            choices.filter { ChoiceSearch.matches(text, label(it), supportingText?.invoke(it), locale) }
        }
    }
    LazyColumn(contentPadding = contentPadding, flingBehavior = AppScrollDefaults.flingBehavior()) {
        if (searching) {
            item(key = SearchKey, contentType = SearchKey) {
                AppSearchField(state = query, modifier = Modifier.padding(bottom = AppTheme.spacing.sm))
            }
        }
        if (onSelectNone != null) {
            item(key = NoneKey, contentType = RowType) {
                AppChoiceRow(
                    title = noneLabel,
                    selected = selected == null,
                    onSelect = {
                        onSelectNone()
                        dismiss()
                    },
                )
            }
        }
        items(shown, key = key, contentType = { RowType }) { choice ->
            AppChoiceRow(
                title = label(choice),
                selected = choice == selected,
                onSelect = {
                    onSelect(choice)
                    dismiss()
                },
                subtitle = supportingText?.invoke(choice),
                enabled = enabled(choice),
            )
        }
        sheetFooterItem()
    }
}

/** The list's own height, at most 0.6 of Large's, and Large: a long list opens at 60 % and grows. */
private val ChoiceDetents: List<SheetDetent> = listOf(
    SheetDetent.Custom("choice-fit") { minOf(contentHeight, maxHeight * AppChoiceSheetDefaults.MaxFitFraction) },
    SheetDetent.Large,
)

private val DefaultProperties = AppSheetProperties()
private const val SearchKey = "choice_search"
private const val NoneKey = "choice_none"
private const val RowType = "choice_row"

@AppComponentPreviews
@Composable
private fun AppChoiceSheetPreview() {
    Box(modifier = Modifier.background(AppTheme.colors.background).fillMaxWidth().height(AppTheme.sizes.rowHeight * 10)) {
        AppChoiceSheet(
            visible = true,
            choices = listOf("English", "Deutsch", "Español", "Français", "Hindi"),
            selected = "Español",
            onSelect = {},
            onDismissRequest = {},
            label = { it },
            title = "Language",
            supportingText = { if (it == "English") "The language the app was written in" else null },
            enabled = { it != "Français" },
        )
    }
}
