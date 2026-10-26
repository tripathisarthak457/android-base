package com.example.finalafter.crnoui3.core.testing.compose.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.finalafter.crnoui3.core.testing.compose.TEST_SDK
import com.github.takahirom.roborazzi.RoborazziOptions
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityViewCheckException
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The audit every generated preview test runs. Native graphics, as the screenshots convention sets:
 * ATF reads the drawn pixels, and without them Roborazzi skips the audit and passes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityAuditTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a control under 48 dp without a label fails`() {
        compose.setContent { Box(Modifier.size(20.dp).clickable {}) }

        assertThrows(AccessibilityViewCheckException::class.java) {
            compose.auditAccessibility(RoborazziOptions())
        }
    }

    @Test
    fun `a labelled control of 48 dp passes`() {
        compose.setContent {
            Box(Modifier.size(48.dp).semantics { contentDescription = "Close" }.clickable {})
        }

        compose.auditAccessibility(RoborazziOptions())
    }
}
