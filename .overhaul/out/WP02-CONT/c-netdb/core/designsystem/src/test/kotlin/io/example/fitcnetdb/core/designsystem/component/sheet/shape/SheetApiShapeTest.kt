package io.example.fitcnetdb.core.designsystem.component.sheet.shape

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.example.fitcnetdb.core.designsystem.component.button.AppButton
import io.example.fitcnetdb.core.designsystem.component.container.AppBottomActionBar
import io.example.fitcnetdb.core.designsystem.component.sheet.AppSheet
import io.example.fitcnetdb.core.designsystem.component.sheet.AppSheetDefaults
import io.example.fitcnetdb.core.designsystem.component.sheet.SheetDetent
import io.example.fitcnetdb.core.designsystem.component.sheet.SheetModality
import io.example.fitcnetdb.core.designsystem.component.sheet.action.AppActionSheet
import io.example.fitcnetdb.core.designsystem.component.sheet.action.SheetAction
import io.example.fitcnetdb.core.designsystem.component.sheet.choice.AppChoiceSheet
import io.example.fitcnetdb.core.designsystem.component.sheet.chrome.AppSheetHeader
import io.example.fitcnetdb.core.designsystem.component.sheet.content.SheetColumn
import io.example.fitcnetdb.core.designsystem.component.sheet.content.SheetDismissConfirmation
import io.example.fitcnetdb.core.designsystem.component.sheet.rememberAppSheetState
import io.example.fitcnetdb.core.designsystem.theme.SheetMaterial
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The common sheets of the specification's acceptance examples, written exactly as an app writes
 * them. A change that makes one of them need another argument or statement breaks this file.
 */
class SheetApiShapeTest {

    @Test
    fun `the common sheets are one call each`() {
        // The blocks below only have to compile; this names them so they are not dead code.
        assertEquals(7, blocks.size)
    }

    private val blocks: List<@Composable () -> Unit> = listOf(
        { QuickSheet() },
        { GrowingSheet() },
        { FormSheet() },
        { PersistentSheet() },
        { GlassSheet() },
        { Choices() },
        { SliverDetent() },
    )
}

@Composable
private fun QuickSheet() {
    var showFilters by remember { mutableStateOf(false) }
    AppSheet(
        visible = showFilters,
        onDismissRequest = { showFilters = false },
        header = { AppSheetHeader(title = stringResource(R.string.orders_filters)) },
    ) {
        FilterList(contentPadding = contentPadding)
    }
}

@Composable
private fun GrowingSheet() {
    var show by remember { mutableStateOf(false) }
    AppSheet(
        visible = show,
        onDismissRequest = { show = false },
        state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
    ) {
        LazyColumn(contentPadding = contentPadding) {
            items(places, key = { it.id }) { PlaceRow(it) }
        }
    }
}

@Composable
private fun FormSheet() {
    var show by remember { mutableStateOf(false) }
    val form = remember { RenameForm() }
    AppSheet(
        visible = show,
        onDismissRequest = { show = false },
        state = rememberAppSheetState(listOf(SheetDetent.FitContent)),
        header = { AppSheetHeader(title = stringResource(R.string.lists_rename)) },
        footer = {
            AppBottomActionBar {
                AppButton(text = save, onClick = onSave, modifier = Modifier.weight(1f))
            }
        },
    ) {
        SheetDismissConfirmation(enabled = form.changed, onDiscard = { show = false })
        SheetColumn { RenameField(form.name, onNameChange) }
    }
}

@Composable
private fun PersistentSheet() {
    AppSheet(
        visible = true,
        onDismissRequest = {},
        state = rememberAppSheetState(
            listOf(SheetDetent.Peek, SheetDetent.Medium, SheetDetent.Large),
        ),
        modality = SheetModality.NonModal(upThrough = SheetDetent.Medium),
        dismissible = false,
        header = { SearchHeader() },
    ) {
        PlaceList(contentPadding = contentPadding)
    }
}

@Composable
private fun GlassSheet() {
    var show by remember { mutableStateOf(false) }
    AppSheet(
        visible = show,
        onDismissRequest = { show = false },
        state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
        style = AppSheetDefaults.style(material = SheetMaterial.Glass),
    ) {
        TrackList(contentPadding = contentPadding)
    }
}

@Composable
private fun SliverDetent() {
    val sliver = SheetDetent.Custom("sliver") { maxOf(44.dp, maxHeight * 0.1f) }
    rememberAppSheetState(listOf(sliver, SheetDetent.Large))
}

@Composable
private fun Choices() {
    var show by remember { mutableStateOf(false) }
    AppActionSheet(
        visible = show,
        actions = actions,
        onDismissRequest = { show = false },
        title = title,
    )
    AppChoiceSheet(
        visible = show,
        choices = languages,
        selected = current,
        onSelect = onLanguage,
        onDismissRequest = { show = false },
        label = { it.displayName },
    )
}

// Stand-ins for an app's own composables, strings and state.

private object R {
    object string {
        const val orders_filters = android.R.string.ok
        const val lists_rename = android.R.string.ok
    }
}

private class Place(val id: Int)

private class RenameForm {
    val changed = false
    val name = ""
}

private val places = listOf(Place(1))
private val save = "Save"
private val onSave: () -> Unit = {}
private val onNameChange: (String) -> Unit = {}
private val actions = listOf(SheetAction(label = "Share", onClick = {}))
private val title = "Photo"
private val languages = listOf(Locale.ENGLISH, Locale.FRENCH)
private val current: Locale = Locale.ENGLISH
private val onLanguage: (Locale) -> Unit = {}

@Composable
private fun FilterList(contentPadding: PaddingValues) = Unit

@Composable
private fun PlaceRow(place: Place) = Unit

@Composable
private fun RenameField(name: String, onNameChange: (String) -> Unit) = Unit

@Composable
private fun SearchHeader() = Unit

@Composable
private fun PlaceList(contentPadding: PaddingValues) = Unit

@Composable
private fun TrackList(contentPadding: PaddingValues) = Unit
