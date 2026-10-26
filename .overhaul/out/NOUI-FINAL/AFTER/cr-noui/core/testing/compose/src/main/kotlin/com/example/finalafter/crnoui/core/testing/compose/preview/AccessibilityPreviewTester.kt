@file:OptIn(ExperimentalRoborazziApi::class)

package com.example.finalafter.crnoui.core.testing.compose.preview

import androidx.activity.compose.setContent
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.github.takahirom.roborazzi.AndroidComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester.TestParameter.JUnit4TestParameter.AndroidPreviewJUnit4TestParameter
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.RoborazziComposeCaptureOption
import com.github.takahirom.roborazzi.RoborazziComposeOptions
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.robolectric.RuntimeEnvironment
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

/**
 * The tester every generated preview test runs (the screenshots convention names it): it renders
 * each preview as Roborazzi does and audits what it drew with Google's Accessibility Test
 * Framework, so every component and screen with a preview is checked without a test of its own.
 *
 * The audit is ATF's latest preset. An error fails the test: a touch target under 48 dp,
 * something a person can act on without a label, two controls on the same spot. A warning is
 * printed in the test's output and does not fail it: contrast, which ATF measures from the drawn
 * pixels and so reports only as a warning (`PaletteContrastTest` holds the theme's colour pairs to
 * WCAG AA exactly), two elements with one description, and ATF's other warnings. A result that is
 * right to accept goes in [SUPPRESSIONS] with its reason, never by turning a check off.
 *
 * Roborazzi composes a preview only when a record, compare or verify task asks for its image.
 * Every other test run composes it here without taking one, so `./gradlew check` renders and
 * audits every preview too.
 */
class AccessibilityPreviewTester : ComposePreviewTester<AndroidPreviewJUnit4TestParameter> {

    private val previews = AndroidComposePreviewTester()

    override fun testParameters(): List<AndroidPreviewJUnit4TestParameter> =
        previews.testParameters()

    override fun test(testParameter: AndroidPreviewJUnit4TestParameter) {
        val capturer = AuditingCapturer(testParameter.composeTestRule)
        AndroidComposePreviewTester(capturer).test(testParameter)
    }
}

/**
 * Results the audit lets pass, each a matcher over ATF's results (`AccessibilityCheckResultUtils`
 * builds them: `matchesChecks`, `matchesViews`, `matchesElements`) and why it was reviewed and
 * accepted. A suppressed result is still printed, marked as suppressed.
 */
private val SUPPRESSIONS: List<Suppression> = emptyList()

private class Suppression(val matcher: Matcher<in AccessibilityViewCheckResult>, val reason: String)

private val CHECKS = RoborazziATFAccessibilityCheckOptions(
    checker = RoborazziATFAccessibilityChecker(
        preset = AccessibilityCheckPreset.LATEST,
        suppressions = ReviewedSuppressions,
    ),
    failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Error,
)

private object ReviewedSuppressions : BaseMatcher<AccessibilityViewCheckResult>() {
    override fun matches(item: Any?): Boolean = SUPPRESSIONS.any { it.matcher.matches(item) }

    override fun describeTo(description: Description) {
        description.appendText("reviewed: " + SUPPRESSIONS.joinToString("; ") { it.reason })
    }
}

private class AuditingCapturer(
    private val rule: ComposeContentTestRule,
) : AndroidComposePreviewTester.Capturer {

    override fun capture(parameter: AndroidComposePreviewTester.CaptureParameter) {
        val options = parameter.roborazziComposeOptions.builder()
            .addOption(AccessibilityAudit(rule, parameter.roborazziOptions))
            .build()
        val roborazziOptions = parameter.roborazziOptions
        if (roborazziOptions.taskType.isEnabled()) {
            parameter.preview.captureRoboImage(parameter.filePath, roborazziOptions, options)
        } else {
            render(parameter.preview, options)
        }
    }
}

/**
 * Runs the audit when the image is about to be taken, so a verify that fails on pixels never hides
 * an accessibility error.
 */
private class AccessibilityAudit(
    private val rule: ComposeContentTestRule,
    private val roborazziOptions: RoborazziOptions,
) : RoborazziComposeCaptureOption {

    override fun beforeCapture() = rule.auditAccessibility(roborazziOptions)

    override fun afterCapture() = Unit
}

/**
 * Audits every window on screen, a dialog's or a popup's included, once the composition is idle,
 * and throws ATF's `AccessibilityViewCheckException` when it finds an error.
 */
internal fun SemanticsNodeInteractionsProvider.auditAccessibility(
    roborazziOptions: RoborazziOptions,
) {
    val roots = onAllNodes(isRoot())
    for (index in roots.fetchSemanticsNodes().indices) {
        roots[index].checkRoboAccessibility(CHECKS, roborazziOptions)
    }
}

/**
 * Composes [preview] as Roborazzi's capture does, in the test rule's activity with the preview's
 * device, locale, font scale and theme applied, and runs the capture options at the moment the
 * image would be taken, without taking it. Robolectric's configuration is restored after, as the
 * capture restores it.
 */
private fun render(
    preview: ComposablePreview<AndroidPreviewInfo>,
    options: RoborazziComposeOptions,
) {
    val qualifiers = RuntimeEnvironment.getQualifiers()
    val fontScale = RuntimeEnvironment.getFontScale()
    try {
        options.createScenario { error("A generated preview test renders in its rule's activity.") }
            .use { scenario ->
                val content = options.configured(scenario) { preview() }
                scenario.onActivity { activity -> activity.setContent(content = content) }
                try {
                    options.beforeCapture()
                } finally {
                    options.afterCapture()
                }
            }
    } finally {
        RuntimeEnvironment.setQualifiers(qualifiers)
        if (RuntimeEnvironment.getFontScale() != fontScale) {
            RuntimeEnvironment.setFontScale(fontScale)
        }
    }
}
