package com.base.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.devtools.screen.CurrentScreen
import com.base.app.core.navigation.NavKeySerialization
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Tells the inspector which destination is on top whenever that changes. Read in a coroutine from
 * the back stack's own state, so a navigation recomposes nothing here.
 *
 * The name is the key's serial name, which R8 leaves as written, where a class name in a minified
 * build would read `a` or `b`.
 */
@Composable
internal fun ReportCurrentScreen(
    top: () -> AppNavKey,
    serialization: NavKeySerialization,
    screen: CurrentScreen,
) {
    val currentTop by rememberUpdatedState(top)
    LaunchedEffect(serialization, screen) {
        snapshotFlow { currentTop() }.collect { key -> screen.report(serialization.screenName(key)) }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun NavKeySerialization.screenName(key: AppNavKey): String {
    val serialName = json.serializersModule.getPolymorphic(AppNavKey::class, key)?.descriptor?.serialName
    return serialName?.substringAfterLast('.') ?: key::class.java.simpleName
}
