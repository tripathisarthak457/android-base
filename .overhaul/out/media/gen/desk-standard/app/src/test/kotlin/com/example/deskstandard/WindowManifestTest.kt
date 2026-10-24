package com.example.deskstandard

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * A window that is resized, folded, rotated or given a keyboard keeps its activity, so no tab is
 * rebuilt; nothing locks the orientation or the size; and the system may open a second window.
 */
class WindowManifestTest {

    private val manifest: Element by lazy {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        factory.newDocumentBuilder().parse(File("src/main/AndroidManifest.xml")).documentElement
    }

    private val main: Element by lazy {
        manifest.children("activity").single { it.androidAttr("name") == ".MainActivity" }
    }

    @Test
    fun `a new window size or night mode does not recreate the activity`() {
        val handled = main.androidAttr("configChanges").split("|").toSet()
        val needed = setOf("orientation", "screenSize", "smallestScreenSize", "screenLayout", "keyboard", "keyboardHidden", "navigation", "uiMode")
        assertTrue("configChanges lacks ${needed - handled}", handled.containsAll(needed))
    }

    @Test
    fun `nothing locks the orientation or the window's size`() {
        for (activity in manifest.children("activity")) {
            assertEquals("", activity.androidAttr("screenOrientation"))
            assertEquals("", activity.androidAttr("resizeableActivity"))
            assertEquals("", activity.androidAttr("maxAspectRatio"))
            assertEquals("", activity.androidAttr("minAspectRatio"))
        }
    }

    @Test
    fun `a desktop opens a sized window, and a second window may be opened`() {
        val layout = main.children("layout").single()
        assertEquals("1024dp", layout.androidAttr("defaultWidth"))
        assertEquals("320dp", layout.androidAttr("minWidth"))
        val property = manifest.children("property").single {
            it.androidAttr("name") == "android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI"
        }
        assertEquals("true", property.androidAttr("value"))
    }

    private fun Element.androidAttr(name: String): String = getAttributeNS(ANDROID, name)

    private fun Element.children(tag: String): List<Element> {
        val nodes = getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
    }
}
