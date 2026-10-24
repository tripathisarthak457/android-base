package com.base.app.shared.navigation

import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.navigation.ShellTab
import com.base.app.shared.resources.Res
// <opt:sample>
import com.base.app.feature.sample.api.SampleListKey
import com.base.app.shared.resources.tab_sample
// </opt:sample>
// <opt:settings>
import com.base.app.feature.settings.api.SettingsKey
import com.base.app.shared.resources.tab_settings
// </opt:settings>
// <generated:shell-tab-imports>

/** The app's shape, in one file. */
internal object AppDestinations {

    /** The tab bar's tabs, in order. The first is where the app starts. */
    val tabs: List<ShellTab> = listOf(
        // <opt:sample>
        ShellTab(
            key = SampleListKey,
            label = Res.string.tab_sample,
            icon = AppIcons.Home,
            selectedIcon = AppIcons.HomeFilled,
        ),
        // </opt:sample>
        // <generated:shell-tabs>
        // Settings sits last because that is where people look for it.
        // <opt:settings>
        ShellTab(
            key = SettingsKey,
            label = Res.string.tab_settings,
            icon = AppIcons.Settings,
            selectedIcon = AppIcons.SettingsFilled,
        ),
        // </opt:settings>
    )
}
