package io.example.audit.core.designsystem.component.feedback

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.example.audit.core.designsystem.TestSdk
import io.example.audit.core.designsystem.component.button.AppButton
import io.example.audit.core.designsystem.component.button.AppIconButton
import io.example.audit.core.designsystem.component.button.ButtonVariant
import io.example.audit.core.designsystem.component.selection.AppChip
import io.example.audit.core.designsystem.foundation.draw.LocalShimmerClock
import io.example.audit.core.designsystem.foundation.draw.ShimmerClock
import io.example.audit.core.designsystem.icon.AppIcons
import io.example.audit.core.designsystem.setThemed
import io.example.audit.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What a frame of a sweep redraws inside a redaction: one bone for each component that draws its
 * own, and nothing under it. A label or icon covered by its button's, chip's, badge's, pill's or
 * avatar's bone is never shown, so a bone of its own would be redrawn on every frame for nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TestSdk])
class RedactedBoneCountTest {

    @get:Rule
    val rule = createComposeRule()

    private var clock: ShimmerClock? = null

    @Before
    fun holdTheClock() {
        rule.mainClock.autoAdvance = false
    }

    @Test
    fun `a component that draws its own bone is one bone to the sweep, shown or redacted again`() {
        var redacted by mutableStateOf(true)
        rule.setThemed {
            clock = LocalShimmerClock.current
            AppRedacted(active = redacted) {
                Column {
                    AppButton(text = "Follow", onClick = {}, leadingIcon = AppIcons.Star)
                    AppIconButton(
                        AppIcons.Close,
                        contentDescription = "Close",
                        onClick = {},
                        variant = ButtonVariant.Filled,
                    )
                    AppChip(label = "Nearby", onClick = {}, leadingIcon = AppIcons.Search, onRemove = {})
                    AppBadge(count = 3)
                    AppStatusPill(text = "Delivered", status = AppTheme.colors.success, icon = AppIcons.Check)
                    AppAvatar(name = "Ada Lovelace")
                }
            }
        }
        assertBones(6)

        redacted = false
        assertBones(0)

        redacted = true
        assertBones(6)
    }

    @Test
    fun `a component with no container leaves its label and icons to stand for it`() {
        rule.setThemed {
            clock = LocalShimmerClock.current
            AppRedacted(active = true) {
                Column {
                    AppButton(
                        text = "Skip",
                        onClick = {},
                        variant = ButtonVariant.Plain,
                        leadingIcon = AppIcons.Close,
                    )
                    AppIconButton(AppIcons.Search, contentDescription = "Search", onClick = {})
                }
            }
        }
        // The plain button's icon and label, and the plain icon button's glyph.
        assertBones(3)
    }

    /** Recomposes after a change, with the clock held, and counts the bones the sweep redraws. */
    private fun assertBones(expected: Int) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        assertEquals(expected, clock?.boneCount)
    }
}
