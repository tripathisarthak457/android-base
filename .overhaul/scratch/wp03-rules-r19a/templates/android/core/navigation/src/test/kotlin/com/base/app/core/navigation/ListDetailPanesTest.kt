package com.base.app.core.navigation

import com.base.app.core.navigation.NavPane.Detail
import com.base.app.core.navigation.NavPane.List
import com.base.app.core.navigation.NavPane.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListDetailPanesTest {

    @Test
    fun `a list on top shows beside an empty detail pane`() {
        assertEquals(1 to null, listDetailPanes(listOf(Single, List)))
    }

    @Test
    fun `a detail over its list shows beside it`() {
        assertEquals(0 to 1, listDetailPanes(listOf(List, Detail)))
    }

    @Test
    fun `details opened one after another keep the list and show the newest`() {
        assertEquals(1 to 4, listDetailPanes(listOf(Single, List, Detail, Detail, Detail)))
    }

    @Test
    fun `a single screen on top is not a pair`() {
        assertNull(listDetailPanes(listOf(List, Detail, Single)))
    }

    @Test
    fun `a detail with no list under it is shown on its own`() {
        assertNull(listDetailPanes(listOf(Single, Detail)))
        assertNull(listDetailPanes(listOf(Detail)))
    }

    @Test
    fun `an empty stack is not a pair`() {
        assertNull(listDetailPanes(emptyList()))
    }
}
