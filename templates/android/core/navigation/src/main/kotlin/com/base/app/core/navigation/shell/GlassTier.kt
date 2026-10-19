package com.base.app.core.navigation.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import com.base.app.core.designsystem.foundation.glass.GlassQuality
import com.base.app.core.designsystem.foundation.glass.LocalGlassQuality
import com.base.app.core.designsystem.foundation.glass.rememberGlassQuality
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.BarTreatment
import com.base.app.core.navigation.ShellConfig
import com.base.app.core.navigation.ShellState

/**
 * Decides the liquid glass tier for the shell's window and provides it to the chrome. With any
 * other bar the tier stays the default and nothing is measured. [ShellConfig.glassQuality] forces
 * a tier, which the performance tooling uses; the tier taken is written to [ShellState.glass] for
 * the dev log.
 */
@Composable
internal fun ProvideGlassTier(config: ShellConfig, state: ShellState, content: @Composable () -> Unit) {
    if (AppTheme.style.bar != BarTreatment.Glass) {
        SideEffect { state.glass = null }
        content()
        return
    }
    val tier: GlassQuality = config.glassQuality ?: rememberGlassQuality()
    SideEffect { state.glass = tier.name }
    CompositionLocalProvider(LocalGlassQuality provides tier, content = content)
}
