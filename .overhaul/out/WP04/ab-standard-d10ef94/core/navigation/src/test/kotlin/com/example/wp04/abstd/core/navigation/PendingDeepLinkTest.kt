package com.example.wp04.abstd.core.navigation

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class PendingDeepLinkTest {

    private val posts = DeepLinkRoute { link ->
        if (link.segments.first() != "posts") return@DeepLinkRoute null
        link.segments.getOrNull(1)?.toIntOrNull()?.let(::Post) ?: FeedTab
    }
    private val pending = PendingDeepLink(setOf(posts))

    @Test
    fun `a custom scheme and an https link read the same`() {
        assertEquals(listOf("posts", "7"), DeepLink.from(Uri.parse("app://posts/7/"))?.segments)
        assertEquals(listOf("posts", "7"), DeepLink.from(Uri.parse("https://example.com/posts/7?utm_source=x"))?.segments)
    }

    @Test
    fun `a held link is handed out once`() {
        assertTrue(pending.hold(Uri.parse("app://posts/7")))

        assertEquals(Post(7), pending.take())
        assertNull(pending.take())
    }

    @Test
    fun `the newest link wins`() {
        pending.hold(Uri.parse("app://posts/1"))
        pending.hold(Uri.parse("app://posts/2"))

        assertEquals(Post(2), pending.take())
    }

    @Test
    fun `a link no feature routes is not held`() {
        assertFalse(pending.hold(Uri.parse("app://orders/3")))
        assertFalse(pending.hold(null))
        assertNull(pending.waiting.value)
    }
}

/** Robolectric refuses SDK 36 and above on a Java 17 toolchain. */
internal const val ROBOLECTRIC_SDK = 35
