package com.example.finalafter.crui3.buildlogic.guard

import org.junit.Assert.assertEquals
import org.junit.Test

/** The cases of the fling rule a single fixture cannot show: what it must flag and what it must leave alone. */
class ScrollRuleCasesTest {

    private fun findings(path: String, code: String): List<Int> =
        CodeRules.check(path, code.trimIndent()).filter { it.rule == "scroll-fling" }.map { it.line }

    @Test
    fun `every kind of container that names the fling passes`() {
        val code = """
            private fun Feed() = LazyColumn(state = list, flingBehavior = AppScrollDefaults.flingBehavior()) { }
            private fun Photos() = LazyRow(flingBehavior = AppScrollDefaults.flingBehavior()) { }
            private fun Grid() = LazyVerticalGrid(
                columns = GridCells.Adaptive(AppTheme.layout.tileMinWidth),
                flingBehavior = AppScrollDefaults.flingBehavior(),
            ) { }
            private fun Pages() = HorizontalPager(state = pager, flingBehavior = AppScrollDefaults.pagerFlingBehavior(pager)) { }
            private fun Detail() = Column(
                Modifier.verticalScroll(rememberScrollState(), flingBehavior = AppScrollDefaults.flingBehavior()),
            ) { }
        """
        assertEquals(emptyList<Int>(), findings(FEATURE, code))
    }

    @Test
    fun `a container that names no fling is flagged, a lambda-only list too`() {
        val code = """
            private fun Feed() = LazyColumn(state = list) { }
            private fun Photos() = LazyRow { }
            private fun Pages() = HorizontalPager(state = pager) { }
            private fun Detail() = Column(Modifier.verticalScroll(rememberScrollState())) { }
            private fun Table() = Row(Modifier.horizontalScroll(rememberScrollState())) { }
        """
        assertEquals(listOf(1, 2, 3, 4, 5), findings(FEATURE, code))
    }

    @Test
    fun `a placeholder list the person cannot scroll never flings`() {
        val code = """
            private fun Loading() = LazyColumn(
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
            ) { }
        """
        assertEquals(emptyList<Int>(), findings(FEATURE, code))
    }

    @Test
    fun `core ui and the app are held to it, the design system and tests are not`() {
        val code = "private fun List() = LazyColumn { }"
        assertEquals(listOf(1), findings(CORE_UI, code))
        assertEquals(listOf(1), findings(APP, code))
        assertEquals(emptyList<Int>(), findings(DESIGN_SYSTEM, code))
        assertEquals(emptyList<Int>(), findings(FEATURE_TEST, code))
    }

    private companion object {
        const val FEATURE = "feature/orders/src/main/kotlin/A.kt"
        const val FEATURE_TEST = "feature/orders/src/test/kotlin/A.kt"
        const val CORE_UI = "core/ui/src/main/kotlin/A.kt"
        const val APP = "app/src/main/kotlin/A.kt"
        const val DESIGN_SYSTEM = "core/designsystem/src/main/kotlin/A.kt"
    }
}
