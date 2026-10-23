package com.example.videoalone.core.navigation

import android.net.Uri
import java.util.Optional
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
    private val origin = LinkOrigin(scheme = "app", host = "app.example.com")
    private val pending = PendingDeepLink(setOf(posts), Optional.of(origin))

    @Test
    fun `a custom scheme and an https link read the same`() {
        assertEquals(listOf("posts", "7"), segments("app://posts/7/"))
        assertEquals(listOf("posts", "7"), segments("https://app.example.com/posts/7?utm_source=x"))
    }

    @Test
    fun `scheme and host are compared ignoring case`() {
        assertEquals(listOf("posts", "7"), segments("APP://posts/7"))
        assertEquals(listOf("posts", "7"), segments("HTTPS://App.Example.com/posts/7"))
    }

    @Test
    fun `only the app's own links are read`() {
        val foreign = listOf(
            "https://example.com/posts/7",
            "https://app.example.com.evil.test/posts/7",
            "https://evil.test/app.example.com/posts/7",
            "http://app.example.com/posts/7",
            "http://posts/7",
            "content://posts/7",
            "other://posts/7",
            "posts/7",
        )
        for (link in foreign) {
            assertNull(link, DeepLink.from(Uri.parse(link), origin))
            assertFalse(link, pending.hold(Uri.parse(link)))
        }
        assertNull(pending.waiting.value)
    }

    @Test
    fun `without a link origin nothing is held`() {
        val noLinks = PendingDeepLink(setOf(posts), Optional.empty())

        assertFalse(noLinks.hold(Uri.parse("app://posts/7")))
        assertFalse(noLinks.hold(Uri.parse("https://app.example.com/posts/7")))
        assertNull(noLinks.waiting.value)
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

    private fun segments(link: String) = DeepLink.from(Uri.parse(link), origin)?.segments
}

/** Robolectric refuses SDK 36 and above on a Java 17 toolchain. */
internal const val ROBOLECTRIC_SDK = 35
