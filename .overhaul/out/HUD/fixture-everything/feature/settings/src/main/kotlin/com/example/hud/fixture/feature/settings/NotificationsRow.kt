package com.example.hud.fixture.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.hud.fixture.core.designsystem.component.container.AppCard
import com.example.hud.fixture.core.designsystem.component.container.AppListItem
import com.example.hud.fixture.core.designsystem.component.text.AppIcon
import com.example.hud.fixture.core.designsystem.icon.AppIcons
import com.example.hud.fixture.core.designsystem.theme.AppTheme
import com.example.hud.fixture.core.notification.permission.NotificationAccess
import com.example.hud.fixture.core.notification.permission.rememberNotificationPermission

/**
 * Whether the app may post notifications, and the way to change it: the system's question while it
 * will still ask, the app's notification settings once it will not.
 */
@Composable
internal fun NotificationsRow() {
    val permission = rememberNotificationPermission()
    val access = permission.access
    AppCard(contentPadding = PaddingValues(0.dp)) {
        AppListItem(
            title = stringResource(R.string.settings_notifications),
            supporting = stringResource(
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
