package com.example.finalafter.crui2.catalog.sections.sheets

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.finalafter.crui2.catalog.CatalogGroup
import com.example.finalafter.crui2.core.designsystem.component.button.AppButton
import com.example.finalafter.crui2.core.designsystem.component.button.ButtonVariant
import com.example.finalafter.crui2.core.designsystem.component.container.AppBottomActionBar
import com.example.finalafter.crui2.core.designsystem.component.list.AppToggleRow
import com.example.finalafter.crui2.core.designsystem.component.sheet.AppSheet
import com.example.finalafter.crui2.core.designsystem.component.sheet.SheetDetent
import com.example.finalafter.crui2.core.designsystem.component.sheet.action.AppActionSheet
import com.example.finalafter.crui2.core.designsystem.component.sheet.action.SheetAction
import com.example.finalafter.crui2.core.designsystem.component.sheet.action.rememberSheetAnchor
import com.example.finalafter.crui2.core.designsystem.component.sheet.action.sheetAnchor
import com.example.finalafter.crui2.core.designsystem.component.sheet.choice.AppChoiceSheet
import com.example.finalafter.crui2.core.designsystem.component.sheet.chrome.AppSheetHeader
import com.example.finalafter.crui2.core.designsystem.component.sheet.content.sheetFooterItem
import com.example.finalafter.crui2.core.designsystem.component.sheet.rememberAppSheetState
import com.example.finalafter.crui2.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.finalafter.crui2.core.designsystem.icon.AppIcons

/** The ready-made sheets: an action sheet and a choice sheet, and a multi-select built from AppSheet. */
@Composable
internal fun PresetDemos() {
    CatalogGroup(
        "Actions",
        caption = "Destructive, disabled and selected rows, with Cancel apart. On a window Medium wide or more it is a " +
            "card beside the button that opened it.",
    ) {
        Triggers { ActionsDemo() }
    }
    CatalogGroup(
        "Choices",
        caption = "A list with a checkmark, supporting text, a disabled row and None; from six choices a search that " +
            "ignores case and accents.",
    ) {
        Triggers {
            ChoiceDemo()
            MultiChoiceDemo()
        }
    }
}

@Composable
private fun ActionsDemo() {
    var shown by remember { mutableStateOf(false) }
    var archived by remember { mutableStateOf(false) }
    val anchor = rememberSheetAnchor()
    DemoTrigger("actions", "Photo actions", onClick = { shown = true }, modifier = Modifier.sheetAnchor(anchor))
    AppActionSheet(
        visible = shown,
        title = "IMG_2041",
        message = "Taken today at 09:14",
        actions = listOf(
            SheetAction(label = "Share", icon = AppIcons.Share, onClick = {}),
            SheetAction(label = "Archived", selected = archived, onClick = { archived = !archived }),
            SheetAction(label = "Print", enabled = false, onClick = {}),
            SheetAction(label = "Delete", icon = AppIcons.Trash, destructive = true, onClick = {}),
        ),
        onDismissRequest = { shown = false },
        anchor = anchor,
    )
}

@Composable
private fun ChoiceDemo() {
    var shown by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf<Language?>(Languages[3]) }
    DemoTrigger("choice", language?.name ?: "No language", onClick = { shown = true })
    AppChoiceSheet(
        visible = shown,
        choices = Languages,
        selected = language,
        onSelect = { language = it },
        onDismissRequest = { shown = false },
        label = { it.name },
        title = "Language",
        supportingText = { it.english },
        enabled = { it.name != "Klingon" },
        onSelectNone = { language = null },
        key = { it.name },
    )
}

/** Several choices at once: built on AppSheet, with Done in the footer. */
@Composable
private fun MultiChoiceDemo() {
    var shown by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(setOf("Español")) }
    DemoTrigger("choice_multi", "Several languages", onClick = { shown = true })
    AppSheet(
        visible = shown,
        onDismissRequest = { shown = false },
        state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
        header = { AppSheetHeader(title = "Languages", trailing = null) },
        footer = {
            AppBottomActionBar {
                AppButton(
                    text = "Done (${picked.size})",
                    onClick = { dismiss() },
                    variant = ButtonVariant.Filled,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        LazyColumn(contentPadding = contentPadding, flingBehavior = AppScrollDefaults.flingBehavior()) {
            items(Languages, key = { it.name }, contentType = { "language" }) { language ->
                AppToggleRow(
                    title = language.name,
                    subtitle = language.english,
                    checked = language.name in picked,
                    onCheckedChange = { on -> picked = if (on) picked + language.name else picked - language.name },
                )
            }
            sheetFooterItem()
        }
    }
}

private class Language(val name: String, val english: String)

private val Languages = listOf(
    "Afrikaans" to "Afrikaans", "Bahasa Indonesia" to "Indonesian", "Català" to "Catalan", "Čeština" to "Czech",
    "Dansk" to "Danish", "Deutsch" to "German", "Eesti" to "Estonian", "English" to "English",
    "Español" to "Spanish", "Euskara" to "Basque", "Français" to "French", "Gaeilge" to "Irish",
    "Galego" to "Galician", "Hrvatski" to "Croatian", "Íslenska" to "Icelandic", "Italiano" to "Italian",
    "Klingon" to "Not available", "Latviešu" to "Latvian", "Lietuvių" to "Lithuanian", "Magyar" to "Hungarian",
    "Nederlands" to "Dutch", "Norsk" to "Norwegian", "Polski" to "Polish", "Português" to "Portuguese",
    "Română" to "Romanian", "Slovenčina" to "Slovak", "Suomi" to "Finnish", "Svenska" to "Swedish",
    "Tiếng Việt" to "Vietnamese", "Türkçe" to "Turkish",
).map { (name, english) -> Language(name, english) }
