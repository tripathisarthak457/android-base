// <opt:push>
package com.base.app.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.notification.permission.NotificationAccess
import com.base.app.core.notification.permission.rememberNotificationPermission

/**
 * Whether the app may post notifications, and the way to change it: the system's question while it
 * will still ask, the app's notification settings once it will not.
 */
@Composable
internal fun NotificationsRow() {
    val permission = rememberNotificationPermission()
    val access = permission.access
    AppCard(contentPadding = PaddingValues(0.dp)) {
        AppRow(
            title = stringResource(R.string.settings_notifications),
            subtitle = stringResource(
                when (access) {
                    NotificationAccess.Granted -> R.string.settings_notifications_on
                    NotificationAccess.NotGranted -> R.string.settings_notifications_off
                    NotificationAccess.Blocked -> R.string.settings_notifications_blocked
                },
            ),
            onClick = permission::request.takeIf { access != NotificationAccess.Granted },
            leading = { AppIcon(AppIcons.Bell, contentDescription = null, tint = AppTheme.colors.contentTertiary) },
        )
    }
}
// </opt:push>
