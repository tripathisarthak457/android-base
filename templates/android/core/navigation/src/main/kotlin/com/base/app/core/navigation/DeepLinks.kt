package com.base.app.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.base.app.core.common.navigation.AppNavKey
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.getAndUpdate
import java.util.Optional
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull
// <opt:ui_components>
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
// </opt:ui_components>

/**
 * A link, normalised: `baseapp://items/12` and `https://baseapp.example.com/items/12` both read as
 * segments `["items", "12"]`. Path segments rather than a regex, so trailing slashes and tracking
 * parameters do not matter.
 *
 * @param segments the link's path, its first token first, with empty segments dropped.
 * @param parameters the link's query parameters by name; an empty string for a name with no value.
 */
class DeepLink(val segments: List<String>, val parameters: Map<String, String>) {

    companion object {
        /**
         * Null for a link with nothing to route, and for one that is not the app's own: another
         * scheme, plain http, or https from a host other than [origin]'s. The activities links
         * arrive at are exported, and an explicit intent skips the manifest's filters, so any app
         * can hand over any URI; the filters' scheme and host are checked again here.
         */
        fun from(uri: Uri?, origin: LinkOrigin): DeepLink? {
            if (uri == null) return null
            val scheme = uri.scheme ?: return null
            val path = uri.pathSegments.filter { it.isNotBlank() }
            // A custom-scheme link puts its first token in the host; an https link in the path.
            val segments = when {
                scheme.equals(origin.scheme, ignoreCase = true) -> listOfNotNull(uri.host) + path
                scheme.equals("https", ignoreCase = true) &&
                    uri.host.equals(origin.host, ignoreCase = true) -> path
                else -> return null
            }
            if (segments.isEmpty()) return null
            val parameters = uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
            return DeepLink(segments, parameters)
        }
    }
}

/**
 * Where the app's own links come from: its custom scheme, and the host its App Links are verified
 * for. The app binds it from the resources its manifest's link filters name; a project without
 * deep links binds none, and holds no link.
 */
data class LinkOrigin(val scheme: String, val host: String)

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
    /** The destination [link] opens, or null when it is not one of this feature's links. */
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
    origin: Optional<LinkOrigin>,
) {
    private val origin = origin.getOrNull()
    private val link = MutableStateFlow<AppNavKey?>(null)

    /** The newest link wins: it is the one the user just tapped. */
    val waiting: StateFlow<AppNavKey?> = link.asStateFlow()

    /** Holds [uri] when a route knows it; returns whether one did. */
    fun hold(uri: Uri?): Boolean {
        val parsed = origin?.let { DeepLink.from(uri, it) } ?: return false
        val key = routes.firstNotNullOfOrNull { it.keyFor(parsed) } ?: return false
        link.value = key
        return true
    }

    /** The waiting link, handed out once. */
    fun take(): AppNavKey? = link.getAndUpdate { null }
}

// <opt:ui_components>
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
// </opt:ui_components>
// <opt-else:ui_components>
//~ /**
//~  * Opens waiting links on [stack]: the key a link names is pushed over the screen on display, so
//~  * Back returns to it. A link to the screen already on top opens nothing new.
//~  */
//~ @Composable
//~ fun OpenDeepLinks(stack: AppBackStack, pending: PendingDeepLink) {
//~     LaunchedEffect(stack, pending) {
//~         pending.waiting.filterNotNull().collect {
//~             pending.take()?.let { key -> stack.applyNow(NavCommand.Navigate(key)) }
//~         }
//~     }
//~ }
// </opt-else:ui_components>
