package com.example.cmp.deeplink

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * The launcher icon must bring the task back as it was, and a link must still reach the running
 * app. Both hang on how the two activities are declared.
 */
class LinkEntryManifestTest {

    private val activities: Map<String, Element> by lazy {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val nodes = factory.newDocumentBuilder().parse(File("src/main/AndroidManifest.xml")).getElementsByTagName("activity")
        (0 until nodes.length).map { nodes.item(it) as Element }.associateBy { it.androidAttr("name") }
    }

    @Test
    fun `the app's activity is single top and takes no links itself`() {
        val main = activities.getValue(".MainActivity")
        assertEquals("singleTop", main.androidAttr("launchMode"))
        assertTrue(main.viewSchemes().isEmpty())
    }

    @Test
    fun `links land on the trampoline, outside the app's task and Recents`() {
        val link = activities.getValue(".deeplink.LinkActivity")
        assertEquals(listOf("@string/deeplink_scheme", "https"), link.viewSchemes())
        assertEquals("", link.androidAttr("taskAffinity"))
        assertEquals("true", link.androidAttr("excludeFromRecents"))
        assertEquals("true", link.androidAttr("noHistory"))
    }

    private fun Element.androidAttr(name: String): String = getAttributeNS(ANDROID, name)

    private fun Element.viewSchemes(): List<String> =
        children("intent-filter")
            .filter { filter -> filter.children("action").any { it.androidAttr("name") == "android.intent.action.VIEW" } }
            .flatMap { filter -> filter.children("data").map { it.androidAttr("scheme") } }

    private fun Element.children(tag: String): List<Element> {
        val nodes = getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
    }
}
