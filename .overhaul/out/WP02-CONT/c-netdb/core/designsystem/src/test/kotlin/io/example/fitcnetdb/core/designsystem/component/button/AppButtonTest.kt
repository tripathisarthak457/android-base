package io.example.fitcnetdb.core.designsystem.component.button

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.example.fitcnetdb.core.designsystem.TestSdk
import io.example.fitcnetdb.core.designsystem.setThemed
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Line breaking needs real text metrics, which Robolectric has only in native graphics mode.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppButtonTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a label wider than its button at twice the text size wraps and loses nothing`() {
        rule.setThemed {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                Box(Modifier.width(200.dp)) {
                    AppButton(text = Label, onClick = {}, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNode(hasText(Label, ignoreCase = true), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val label = layouts.single()
        assertTrue("lines ${label.lineCount}", label.lineCount > 1)
        assertFalse("the label was cut", label.hasVisualOverflow)
    }

    private companion object {
        const val Label = "Keyboard and TalkBack"
    }
}
