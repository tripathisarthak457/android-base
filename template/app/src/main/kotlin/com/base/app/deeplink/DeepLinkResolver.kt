package com.base.app.deeplink

import android.net.Uri
import com.base.app.core.navigation.AppNavKey
import javax.inject.Inject
import javax.inject.Singleton
// <opt:sample>
import com.base.app.feature.sample.SampleDetailKey
import com.base.app.feature.sample.SampleListKey
// </opt:sample>

/**
 * Turns an incoming link into a destination.
 *
 * ## Both schemes resolve through one function
 *
 * `baseapp://item/12` and `https://baseapp.example.com/item/12` produce the same key. Handling
 * them separately is how the two drift, and the custom-scheme path — the one used by push
 * notifications and rarely opened by hand — is always the one that rots.
 *
 * ## An unrecognised link returns null
 *
 * The caller then leaves the app on its normal start destination rather than showing a blank
 * screen. Links outlive the version of the app that understood them: someone opens a two-year-old
 * email, and landing on home is the right answer.
 *
 * ## It is deliberately dumb about parameters
 *
 * A link carries ids, and the screen fetches. Trusting a link to carry displayable content means
 * trusting whatever the user pasted into the address bar.
 */
@Singleton
class DeepLinkResolver @Inject constructor() {

    fun resolve(uri: Uri?): AppNavKey? {
        if (uri == null) return null

        // Path segments rather than a regex over the whole URL: this ignores trailing slashes,
        // query strings and tracking parameters for free, and those are attached to essentially
        // every link that arrives from an email or a campaign.
        val segments = uri.pathSegments.filter { it.isNotBlank() }
        val host = uri.host.orEmpty()

        // A custom-scheme link puts the first token in the host, an https link puts it in the
        // path. Normalising here is what lets the `when` below read as one set of routes.
        val tokens = if (uri.scheme.equals("https", ignoreCase = true)) segments else listOf(host) + segments

        return when (tokens.firstOrNull()) {
            // <opt:sample>
            "items" -> tokens.getOrNull(1)?.toIntOrNull()
                ?.let(::SampleDetailKey)
                ?: SampleListKey
            // </opt:sample>

            else -> null
        }
    }
}
