package com.example.finalafter.crui3.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper

/**
 * A component in every condition it has to survive: both themes, twice the font size, right to left
 * and a narrow window, each inside [AppPreviewTheme]. Put it on a private preview in the
 * component's own file:
 *
 * ```
 * @AppComponentPreviews
 * @Composable
 * private fun AppTagPreview() {
 *     AppTag(text = "New")
 * }
 * ```
 *
 * The screenshot tests render every function that carries it, so a component that breaks at 2x or
 * right to left fails the build before anyone ships it.
 */
@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Preview(name = "Font 2x", showBackground = true, fontScale = 2f)
@Preview(name = "Right to left", showBackground = true, locale = "ar")
@Preview(name = "Narrow", showBackground = true, widthDp = 320)
@PreviewWrapper(AppPreviewTheme::class)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
annotation class AppComponentPreviews
