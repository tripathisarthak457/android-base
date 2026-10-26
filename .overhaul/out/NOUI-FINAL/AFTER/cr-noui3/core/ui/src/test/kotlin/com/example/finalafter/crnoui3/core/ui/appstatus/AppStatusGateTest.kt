package com.example.finalafter.crnoui3.core.ui.appstatus

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.finalafter.crnoui3.core.designsystem.theme.AppTheme
import com.example.finalafter.crnoui3.core.network.appstatus.AppStatus
import com.example.finalafter.crnoui3.core.testing.compose.TEST_SDK
import com.example.finalafter.crnoui3.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Without components the cover is a placeholder, and the placeholder is the only way out: a 503
 * puts maintenance up until the person asks again, and an update is had only from the store.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class AppStatusGateTest {

    @get:Rule
    val rule = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private var status by mutableStateOf(AppStatus.Maintenance)
    private var retries = 0
    private var contentTaps = 0

    @Test
    fun `during maintenance a tap asks again, once, and reaches nothing underneath`() {
        show()

        cover().assert(clickLabelled(R.string.ui_appstatus_maintenance_action))
        cover().performClick()

        rule.runOnIdle {
            assertEquals(1, retries)
            assertEquals("A tap reached the app under the cover.", 0, contentTaps)
        }
    }

    @Test
    fun `when an update is required a tap opens the store listing, and asks nothing again`() {
        status = AppStatus.UpdateRequired
        show()

        cover().assert(clickLabelled(R.string.ui_appstatus_update_action))
        cover().performClick()

        rule.runOnIdle {
            val opened = shadowOf(app).nextStartedActivity
            assertEquals(Intent.ACTION_VIEW, opened.action)
            assertEquals(Uri.parse("market://details?id=${app.packageName}"), opened.data)
            assertEquals(0, retries)
            assertEquals(0, contentTaps)
        }
    }

    @Test
    fun `while the service is available nothing covers the app`() {
        status = AppStatus.Available
        show()

        cover().assertDoesNotExist()
        rule.onNodeWithTag(CONTENT_TAG).performClick()
        rule.runOnIdle {
            assertEquals(1, contentTaps)
            assertNull(shadowOf(app).nextStartedActivity)
        }
    }

    @Test
    fun `the cover goes as soon as the service is back`() {
        show()
        cover().assertExists()

        rule.runOnIdle { status = AppStatus.Available }

        cover().assertDoesNotExist()
    }

    private fun show() {
        rule.setContent {
            AppTheme {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag(CONTENT_TAG)
                            .pointerInput(Unit) { detectTapGestures { contentTaps++ } },
                    )
                    AppStatusGate(status = status, onRetry = { retries++ })
                }
            }
        }
    }

    private fun cover() = rule.onNodeWithTag(PLACEHOLDER_TAG)

    private fun clickLabelled(label: Int): SemanticsMatcher {
        val text = app.getString(label)
        return SemanticsMatcher("click action labelled \"$text\"") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label == text
        }
    }
}

private const val CONTENT_TAG = "content"
private const val PLACEHOLDER_TAG = "placeholder"
