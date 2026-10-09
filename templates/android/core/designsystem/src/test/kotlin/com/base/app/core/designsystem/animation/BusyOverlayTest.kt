package com.base.app.core.designsystem.animation

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.base.app.core.designsystem.TEST_SDK
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.setThemed
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class BusyOverlayTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a busy form takes the tap before the button inside can`() {
        var submits = 0
        rule.setThemed {
            Column(Modifier.busyOverlay(busy = true)) {
                AppButton(text = "Submit", onClick = { submits++ })
            }
        }
        rule.onNodeWithText("Submit").performClick()
        assertEquals(0, submits)
    }
}
