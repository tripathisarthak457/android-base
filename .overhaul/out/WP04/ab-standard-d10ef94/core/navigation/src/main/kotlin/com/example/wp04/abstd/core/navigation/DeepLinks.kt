package com.example.wp04.abstd.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.example.wp04.abstd.core.common.navigation.AppNavKey
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import javax.inject.Inject

/**
 * A link, normalised: `standard://items/12` and `https://standard.example.com/items/12` both read as
 * segments `["items", "12"]`. Path segments rather than a regex, so trailing slashes and tracking
 * parameters do not matter.
 */
class DeepLink(val segments: List<String>, val parameters: Map<String, String>) {

    companion object {
        /** Null for a link with nothing to route. */
        fun from(uri: Uri?): DeepLink? {
            if (uri == null) return null
            val path = uri.pathSegments.filter { it.isNotBlank() }
            // A custom-scheme link puts its first token in the host; an https link in the path.
            val segments = if (uri.scheme.equals("https", ignoreCase = true)) path else listOfNotNull(uri.host) + path
            if (segments.isEmpty()) return null
            val parameters = uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
            return DeepLink(segments, parameters)
        }
    }
}

/**
 * One feature's links: each feature contributes its own with `@Provides @IntoSet`, so adding a
 * screen and its link touches no file outside the feature.
 *
 * ```
 * @Provides
 * @IntoSet
 * fun sampleLinks(): DeepLinkRoute = DeepLinkRoute { link ->
 *     if (link.segments.first() != "items") return@DeepLinkRoute null
 *     link.segments.getOrNull(1)?.toIntOrNull()?.let(::SampleDetailKey) ?: SampleListKey
 * }
 * ```
 */
fun interface DeepLinkRoute {
    fun keyFor(link: DeepLink): AppNavKey?
}

/**
 * A link waiting for the app to be able to open it. It is held, not navigated to, when it arrives:
 * pushed onto the sign-in or onboarding stack it would load with no session and be gone by the time
 * the user got in. The signed-in shell takes it once, after the gates and once nothing is moving.
 * One per window, kept across its configuration changes, so a rotation in between keeps it and a
 * link opens in the window it was sent to.
 */
@ActivityRetainedScoped
class PendingDeepLink @Inject constructor(
    private val routes: Set<@JvmSuppressWildcards DeepLinkRoute>,
) {
    private val link = MutableStateFlow<AppNavKey?>(null)

    /** The newest link wins: it is the one the user just tapped. */
    val waiting: StateFlow<AppNavKey?> = link.asStateFlow()

    /** Holds [uri] when a route knows it; returns whether one did. */
    fun hold(uri: Uri?): Boolean {
        val parsed = DeepLink.from(uri) ?: return false
        val key = routes.firstNotNullOfOrNull { it.keyFor(parsed) } ?: return false
        link.value = key
        return true
    }

    /** The waiting link, handed out once. */
    fun take(): AppNavKey? = link.getAndUpdate { null }
}

/**
 * Opens waiting links in the signed-in shell: a tab's root selects that tab; another key selects the
 * tab whose feature registered it and is pushed there, or presented above it.
 */
@Composable
fun OpenDeepLinks(state: ShellState, pending: PendingDeepLink) {
    LaunchedEffect(state, pending) {
        pending.waiting.filterNotNull().collect {
            snapshotFlow { state.moving }.first { !it }
            pending.take()?.let(state::open)
        }
    }
}
