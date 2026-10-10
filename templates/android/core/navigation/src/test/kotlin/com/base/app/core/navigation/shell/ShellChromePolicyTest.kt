package com.base.app.core.navigation.shell

import com.base.app.core.designsystem.theme.WindowHeight
import com.base.app.core.designsystem.theme.WindowWidth
import com.base.app.core.navigation.NavigationChrome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellChromePolicyTest {

    @Test
    fun `the chrome follows the window`() {
        val cases = listOf(
            Triple(WindowWidth.Compact, WindowHeight.Medium, NavigationChrome.Bar),
            Triple(WindowWidth.Compact, WindowHeight.Expanded, NavigationChrome.Bar),
            Triple(WindowWidth.Medium, WindowHeight.Expanded, NavigationChrome.Rail),
            Triple(WindowWidth.Expanded, WindowHeight.Medium, NavigationChrome.Rail),
            Triple(WindowWidth.Large, WindowHeight.Medium, NavigationChrome.Sidebar),
            Triple(WindowWidth.ExtraLarge, WindowHeight.Expanded, NavigationChrome.Sidebar),
        )
        for ((width, height, chrome) in cases) {
            assertEquals("$width × $height", chrome, chromeFor(width, height, sidebarOpen = null))
        }
    }

    @Test
    fun `a short window gets a rail whatever its width`() {
        for (width in WindowWidth.entries) {
            assertEquals(width.name, NavigationChrome.Rail, chromeFor(width, WindowHeight.Compact, sidebarOpen = null))
        }
    }

    @Test
    fun `the person's choice opens or folds the sidebar where one fits`() {
        assertEquals(NavigationChrome.Rail, chromeFor(WindowWidth.Large, WindowHeight.Medium, sidebarOpen = false))
        assertEquals(NavigationChrome.Sidebar, chromeFor(WindowWidth.Expanded, WindowHeight.Medium, sidebarOpen = true))
        assertEquals(NavigationChrome.Rail, chromeFor(WindowWidth.Medium, WindowHeight.Medium, sidebarOpen = true))
        assertEquals(NavigationChrome.Bar, chromeFor(WindowWidth.Compact, WindowHeight.Medium, sidebarOpen = true))
        assertTrue(sidebarAvailable(WindowWidth.Expanded, WindowHeight.Medium))
        assertFalse(sidebarAvailable(WindowWidth.Medium, WindowHeight.Expanded))
        assertFalse(sidebarAvailable(WindowWidth.ExtraLarge, WindowHeight.Compact))
    }

    @Test
    fun `tabs past what the chrome holds go under More`() {
        assertEquals(5, NavigationChrome.Bar.tabsShown(5))
        assertEquals(4, NavigationChrome.Bar.tabsShown(6))
        assertEquals(4, NavigationChrome.Bar.tabsShown(9))
        assertEquals(7, NavigationChrome.Rail.tabsShown(7))
        assertEquals(6, NavigationChrome.Rail.tabsShown(8))
        assertEquals(9, NavigationChrome.Sidebar.tabsShown(9))
    }
}
