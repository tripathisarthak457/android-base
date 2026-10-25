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
            import kotlinx.coroutines.flow.Flow
        """

        assertEquals(
            listOf("4 designsystem-imports", "5 designsystem-imports", "6 designsystem-imports"),
            rules(COMPONENT, source),
        )
    }

    @Test
    fun `a component reaches no storage, network, intent, toast or log`() {
        val source = """
            package com.acme.core.designsystem.component
            import android.content.SharedPreferences
            import java.io.File
            import java.net.URL
            import android.content.Intent
            import android.widget.Toast
            import android.util.Log
        """

        assertEquals((2..7).map { "$it designsystem-imports" }, rules(COMPONENT, source))
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
    fun `a feature and core ui read type from the theme`() {
        val source = """
            private val label = 14.sp
            private val body = TextStyle(fontSize = 17.sp)
            private val title = AppTheme.typography.title.copy(fontWeight = FontWeight.Bold)
        """

        assertEquals(listOf("1 raw-type", "2 raw-type"), rules(FEATURE, source))
        assertEquals(listOf("1 raw-type", "2 raw-type"), rules(CORE_UI, source))
    }

    // <opt:!ui_components>
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

    // </opt:!ui_components>
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

    // <opt:!ui_components>
    @Test
    fun `a component comes with a preview, a private helper and the plumbing need none`() {
        val unpreviewed = """
            package com.acme.core.designsystem.component
            @Composable
            fun Tag(text: String, modifier: Modifier = Modifier) = Box(modifier)
        """
        val previewed = """
            package com.acme.core.designsystem.component
            @Composable
            fun Tag(text: String, modifier: Modifier = Modifier) = Box(modifier)
            @AppComponentPreviews
            @Composable
            private fun TagPreview() = Tag("New")
        """
        val helper = """
            package com.acme.core.designsystem.component
            @Composable
            private fun Dot(modifier: Modifier) = Box(modifier)
        """

        assertEquals(listOf("1 component-preview"), rules(COMPONENT, unpreviewed))
        assertEquals(emptyList<String>(), rules(COMPONENT, previewed))
        assertEquals(emptyList<String>(), rules(COMPONENT, helper))
        assertEquals(emptyList<String>(), rules(ADAPTIVE, unpreviewed))
    }

    // </opt:!ui_components>
    @Test
    fun `an offscreen layer carries the reason it is worth a texture`() {
        val source = """
            private fun Modifier.frosted() = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            private fun Modifier.soft() = blur(AppTagDefaults.Blur)
            private val glow = BlurEffect(4f, 4f)
            private fun Modifier.bent(effect: android.graphics.RenderEffect) = this
            private fun Modifier.dimmed() = graphicsLayer { compositingStrategy = CompositingStrategy.ModulateAlpha }
            // allow(offscreen-layer): the edge fade masks the content it draws, which needs its own layer
            private fun Modifier.faded() = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        """
        val shell = "private fun Modifier.card() = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }"

        assertEquals(
            listOf("1 offscreen-layer", "2 offscreen-layer", "3 offscreen-layer", "4 offscreen-layer"),
            rules(COMPONENT_HELPER, source),
        )
        assertEquals(listOf("1 offscreen-layer"), rules(NAVIGATION, shell))
        assertEquals(emptyList<String>(), rules(FEATURE_TEST, shell))
    }

    @Test
    fun `the modifier a composable takes is used`() {
        val source = """
            @Composable
            private fun Tag(text: String, modifier: Modifier = Modifier) {
                Box(modifier = Modifier.fillMaxWidth()) { Label(text) }
            }

            @Composable
            private fun Chip(text: String, modifier: Modifier = Modifier) = Box(modifier) { Label(text) }

            @Composable
            private fun Badge(
                count: Int,
                modifier: Modifier = Modifier,
            ) {
                Box(Modifier.padding(AppTheme.spacing.small).then(modifier)) { Label("${'$'}count") }
            }

            @Composable
            private fun Dot(modifier: Modifier) = Box(Modifier.size(AppTheme.sizes.dot))
        """

        assertEquals(listOf("2 modifier-unused", "18 modifier-unused"), rules(COMPONENT_HELPER, source))
        assertEquals(listOf("2 modifier-unused", "18 modifier-unused"), rules(CORE_UI, source))
    }

    @Test
    fun `an animated value is read in a layer, layout or draw lambda, not in composition`() {
        val source = """
            @Composable
            private fun Fade(visible: Boolean, modifier: Modifier = Modifier) {
                val fade = animateFloatAsState(if (visible) 1f else 0f)
                val lift = animateDpAsState(AppTheme.spacing.small)
                Box(modifier.alpha(fade.value))
                Box(modifier.padding(top = lift.value))
                Box(modifier.graphicsLayer { alpha = fade.value })
                Box(modifier.offset { IntOffset(0, lift.value.roundToPx()) })
            }
        """

        assertEquals(listOf("5 composition-read", "6 composition-read"), rules(COMPONENT_HELPER, source))
        assertEquals(listOf("5 composition-read", "6 composition-read"), rules(FEATURE, source))
    }

    @Test
    fun `the design system keys its lazy rows, collects with the lifecycle and loads images without subcomposition`() {
        val source = """
            @Composable
            private fun Rows(rows: List<Row>, modifier: Modifier = Modifier) {
                LazyColumn(modifier) {
                    item(contentType = "header") { Header() }
                    items(rows) { Row(it) }
                }
                val ready by readiness.collectAsState()
                SubcomposeAsyncImage(model = url, contentDescription = null)
            }
        """

        assertEquals(
            listOf("5 lazy-content-type", "5 lazy-key", "7 collect-as-state", "8 subcompose-image"),
            rules(COMPONENT_HELPER, source),
        )
    }

    @Test
    fun `a value the whole tree reads is the theme's, so features and core ui create no composition local`() {
        val source = """
            internal val LocalOrdersFilter = compositionLocalOf { OrdersFilter.All }
            internal val LocalOrdersClock = staticCompositionLocalOf<Clock> { error("no clock") }
        """

        assertEquals(listOf("1 feature-composition-local", "2 feature-composition-local"), rules(FEATURE, source))
        assertEquals(listOf("1 feature-composition-local", "2 feature-composition-local"), rules(CORE_UI, source))
        assertEquals(emptyList<String>(), rules(THEME, source))
    }

    @Test
    fun `a description is null for decoration, never empty`() {
        val source = """
            @Composable
            private fun Avatar(url: String, modifier: Modifier = Modifier) {
                AsyncImage(model = url, contentDescription = "", modifier = modifier)
                Icon(AppIcons.Close, contentDescription = null)
                Box(Modifier.semantics { contentDescription = "" })
            }
        """

        assertEquals(listOf("3 empty-description", "5 empty-description"), rules(FEATURE, source))
        assertEquals(listOf("3 empty-description", "5 empty-description"), rules(COMPONENT_HELPER, source))
        assertEquals(emptyList<String>(), rules(FEATURE_TEST, source))
    }

    private companion object {
        const val DESIGN = "core/designsystem/src/main/kotlin/com/acme/core/designsystem"
        const val COMPONENT = "$DESIGN/component/Tag.kt"
        const val COMPONENT_HELPER = "$DESIGN/component/TagParts.kt"
        const val THEME = "$DESIGN/theme/Tokens.kt"
        // <opt:!ui_components>
        const val ADAPTIVE = "$DESIGN/foundation/adaptive/Panes.kt"
        // </opt:!ui_components>
        const val NAVIGATION = "core/navigation/shell/src/main/kotlin/com/acme/core/navigation/shell/Card.kt"
        const val CORE_UI = "core/ui/src/main/kotlin/com/acme/core/ui/Rows.kt"
        const val FEATURE = "feature/orders/src/main/kotlin/com/acme/feature/orders/Orders.kt"
        const val FEATURE_TEST = "feature/orders/src/test/kotlin/com/acme/feature/orders/OrdersTest.kt"
    }
}
