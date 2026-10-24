package com.base.app.buildlogic.guard

import org.junit.Assert.assertEquals
import org.junit.Test

/** The component rules' cases: what each flags in a component or a feature, what it leaves. */
class ComponentRuleCasesTest {

    private fun rules(path: String, source: String): List<String> =
        CodeRules.check(path, source.trimIndent()).map { "${it.line} ${it.rule}" }

    @Test
    fun `a component sees Compose and its own design system, nothing else of the project's`() {
        val source = """
            package com.acme.core.designsystem.component
            import androidx.compose.runtime.Composable
            import com.acme.core.designsystem.theme.AppTheme
            import com.acme.data.orders.api.OrdersRepository
            import androidx.lifecycle.viewmodel.compose.viewModel
            import kotlinx.coroutines.flow.StateFlow
            import android.util.Log
        """

        assertEquals(
            listOf(
                "4 designsystem-imports",
                "5 designsystem-imports",
                "6 designsystem-imports",
                "7 designsystem-imports",
            ),
            rules(COMPONENT, source),
        )
    }

    @Test
    fun `a component takes values and callbacks, never a ViewModel, data or a Flow`() {
        val source = """
            @Composable
            fun Tag(
                text: String,
                onClick: () -> Unit,
                orders: OrdersViewModel,
                rows: StateFlow<List<String>>,
                store: OrdersRepository,
            ) {}
        """

        assertEquals(
            listOf("5 stateless-params", "6 stateless-params", "7 stateless-params"),
            rules(COMPONENT_HELPER, source),
        )
    }

    @Test
    fun `only a feature's Route reaches for its ViewModel`() {
        val source = """
            @Composable
            internal fun OrdersRoute(viewModel: OrdersViewModel = hiltViewModel()) {}

            @Composable
            private fun OrdersBody() {
                val viewModel: OrdersViewModel = hiltViewModel()
            }
        """

        assertEquals(listOf("6 stateless-params"), rules(FEATURE, source))
    }

    @Test
    fun `a component writes its metrics in its Defaults object, the rest comes from the theme`() {
        val source = """
            object TagDefaults {
                val Height = 32.dp
            }

            private val inset = 6.dp
            private val stroke = 1.dp
            private val label = 14.sp
            private val accent = Color(0xFF3366FF)
            private val settle = tween<Float>(300)
        """

        assertEquals(
            listOf("5 raw-dp", "7 raw-type", "8 raw-color", "9 raw-tween"),
            rules(COMPONENT_HELPER, source),
        )
    }

    @Test
    fun `the theme is where the raw values are written`() {
        val source = """
            val Accent = Color(0xFF3366FF)
            val Gutter = 16.dp
            val Body = TextStyle(fontSize = 17.sp)
            val Settle = tween<Float>(300)
        """

        assertEquals(emptyList<String>(), rules(THEME, source))
    }

    @Test
    fun `a feature arranges components and draws nothing of its own`() {
        val source = """
            private fun Modifier.card() = background(color).border(stroke)
            private fun Modifier.lifted() = graphicsLayer { translationY = offset() }.clip(shape)
            @Composable
            private fun Caption() = BasicText(text)
        """

        assertEquals(listOf("1 feature-primitive", "4 feature-primitive"), rules(FEATURE, source))
    }

    @Test
    fun `a component comes with a preview, a private helper and the plumbing need none`() {
        val unpreviewed = """
            package com.acme.core.designsystem.component
            @Composable
            fun Tag(text: String, modifier: Modifier = Modifier) {}
        """
        val previewed = """
            package com.acme.core.designsystem.component
            @Composable
            fun Tag(text: String, modifier: Modifier = Modifier) {}
            @AppComponentPreviews
            @Composable
            private fun TagPreview() = Tag("New")
        """
        val helper = """
            package com.acme.core.designsystem.component
            @Composable
            private fun Dot(modifier: Modifier) {}
        """

        assertEquals(listOf("1 component-preview"), rules(COMPONENT, unpreviewed))
        assertEquals(emptyList<String>(), rules(COMPONENT, previewed))
        assertEquals(emptyList<String>(), rules(COMPONENT, helper))
        assertEquals(emptyList<String>(), rules(ADAPTIVE, unpreviewed))
    }

    private companion object {
        const val DESIGN = "core/designsystem/src/main/kotlin/com/acme/core/designsystem"
        const val COMPONENT = "$DESIGN/component/Tag.kt"
        const val COMPONENT_HELPER = "$DESIGN/component/TagParts.kt"
        const val THEME = "$DESIGN/theme/Tokens.kt"
        const val ADAPTIVE = "$DESIGN/foundation/adaptive/Panes.kt"
        const val FEATURE = "feature/orders/src/main/kotlin/com/acme/feature/orders/Orders.kt"
    }
}
