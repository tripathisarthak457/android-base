package io.example.fitclocal.core.designsystem.component.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.example.fitclocal.core.designsystem.component.sheet.chrome.AppSheetHeader
import io.example.fitclocal.core.designsystem.component.sheet.content.SheetColumn
import io.example.fitclocal.core.designsystem.theme.AppTheme
import io.example.fitclocal.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope

/**
 * A screen with one in-screen sheet and a Back handler beneath it at default priority, which hears
 * only the Back the sheet lets through. It counts what tests of the sheet's contract ask about.
 */
internal class SheetTestScreen(
    private val detents: List<SheetDetent> = listOf(SheetDetent.Medium, SheetDetent.Large),
    private val dismissible: Boolean = true,
) {
    var visible by mutableStateOf(true)
    var dismissRequests = 0
        private set
    var hiddenCalls = 0
        private set
    var poppedBeneath = 0
        private set
    var contentMeasures = 0
        private set
    lateinit var state: AppSheetState
        private set
    lateinit var scope: CoroutineScope
        private set
    lateinit var sheetScope: AppSheetScope
        private set

    @Composable
    fun Content() {
        AppTheme(mode = ThemeMode.Light, reduceMotion = false) {
            scope = rememberCoroutineScope()
            state = rememberAppSheetState(detents)
            NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None)) { poppedBeneath++ }
            AppSheet(
                visible = visible,
                onDismissRequest = {
                    dismissRequests++
                    visible = false
                },
                state = state,
                dismissible = dismissible,
                onHidden = { hiddenCalls++ },
                header = { AppSheetHeader(title = "Sheet") },
            ) {
                sheetScope = this
                SheetColumn {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .layout { measurable, constraints ->
                                contentMeasures++
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            },
                    )
                }
            }
        }
    }
}
