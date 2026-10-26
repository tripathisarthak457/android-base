package com.example.finalafter.crnoui3.buildlogic.guard

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Without a shell, the bars a scrolling screen must clear are the system's: padding from their
 * insets inside the scroll passes, as does the contentPadding a shell hands the screen later.
 */
class ScrollChromeCasesTest {

    private fun findings(code: String): List<Int> =
        CodeRules.check(FEATURE, code.trimIndent()).filter { it.rule == "scroll-chrome" }.map { it.line }

    @Test
    fun `insets inside the scroll pass, on a list and on a column`() {
        val code = """
            private fun Feed() = LazyColumn(
                contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
                flingBehavior = fling,
            ) { }
            private fun Grid() = LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = WindowInsets.safeDrawing
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    .asPaddingValues(),
                flingBehavior = fling,
            ) { }
            private fun Handed(contentPadding: PaddingValues) =
                LazyColumn(contentPadding = contentPadding, flingBehavior = fling) { }
            private fun Detail() = Column(
                Modifier
                    .verticalScroll(rememberScrollState(), flingBehavior = fling)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) { }
            private fun Notes() = Column(
                Modifier.verticalScroll(rememberScrollState(), flingBehavior = fling).safeDrawingPadding(),
            ) { }
            private fun About() = Column(
                Modifier
                    .verticalScroll(rememberScrollState(), flingBehavior = fling)
                    .padding(WindowInsets.systemBars.asPaddingValues()),
            ) { }
        """
        assertEquals(emptyList<Int>(), findings(code))
    }

    @Test
    fun `insets around the scroll, or a fixed padding, leave content under the bars`() {
        val code = """
            private fun Feed() = LazyColumn(
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                flingBehavior = fling,
            ) { }
            private fun Detail() = Column(
                Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState(), flingBehavior = fling),
            ) { }
            private fun Grid() = LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(AppTheme.spacing.gutter),
                flingBehavior = fling,
            ) { }
        """
        assertEquals(listOf(1, 8, 10), findings(code))
    }

    private companion object {
        const val FEATURE = "feature/orders/src/main/kotlin/A.kt"
    }
}
