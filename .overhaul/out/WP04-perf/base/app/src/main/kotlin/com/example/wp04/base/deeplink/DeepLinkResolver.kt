package com.example.wp04.base.deeplink

import android.net.Uri
import com.example.wp04.base.core.navigation.AppNavKey
import javax.inject.Inject
import javax.inject.Singleton
import com.example.wp04.base.feature.sample.SampleDetailKey
import com.example.wp04.base.feature.sample.SampleListKey

/**
 * Turns an incoming link into a destination. `wp04base://items/12` and
 * `https://wp04base.example.com/items/12` produce the same key.
 */
@Singleton
class DeepLinkResolver @Inject constructor() {
    fun resolve(uri: Uri?): AppNavKey? {
        if (uri == null) return null

        // Path segments rather than a regex, so trailing slashes and tracking parameters are
        // ignored.
        val segments = uri.pathSegments.filter { it.isNotBlank() }
        val host = uri.host.orEmpty()

        // A custom-scheme link puts the first token in the host, an https link puts it in the
        // path. Normalising here is what lets the `when` below read as one set of routes.
        val tokens = if (uri.scheme.equals("https", ignoreCase = true)) segments else listOf(host) + segments

        return when (tokens.firstOrNull()) {
            "items" -> tokens.getOrNull(1)?.toIntOrNull()
                ?.let(::SampleDetailKey)
                ?: SampleListKey

            else -> null
        }
    }
}
