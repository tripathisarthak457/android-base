package com.example.cmp.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper

/**
 * A screen at every size it has to work at: a phone upright and sideways, a foldable, a tablet, a
 * desktop window, twice the font size and the dark theme, each inside [AppPreviewTheme]. Put it on
 * the stateless screen's preview, never on the route:
 *
 * ```
 * @AppScreenPreviews
 * @Composable
 * private fun OrdersScreenPreview() {
 *     OrdersScreen(state = OrdersState(items = previewOrders), onEvent = {})
 * }
 * ```
 *
 * The screenshot tests render every function that carries it at these sizes, so a layout that only
 * works on a phone fails there before anyone ships it.
 */
@Preview(name = "Phone", device = Devices.PHONE)
@Preview(name = "Phone landscape", device = "spec:width=891dp,height=411dp")
@Preview(name = "Foldable", device = Devices.FOLDABLE)
@Preview(name = "Tablet", device = Devices.TABLET)
@Preview(name = "Desktop", device = Devices.DESKTOP)
@Preview(name = "Phone, font 2x", device = Devices.PHONE, fontScale = 2f)
@Preview(name = "Phone, dark", device = Devices.PHONE, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@PreviewWrapper(AppPreviewTheme::class)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
public annotation class AppScreenPreviews
