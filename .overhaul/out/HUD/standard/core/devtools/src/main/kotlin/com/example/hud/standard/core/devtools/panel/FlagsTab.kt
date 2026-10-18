package com.example.hud.standard.core.devtools.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hud.standard.core.designsystem.component.button.AppButton
import com.example.hud.standard.core.designsystem.component.button.ButtonSize
import com.example.hud.standard.core.designsystem.component.button.ButtonVariant
import com.example.hud.standard.core.designsystem.component.selection.AppSwitch
import com.example.hud.standard.core.designsystem.component.text.AppMonoText
import com.example.hud.standard.core.designsystem.component.text.AppText
import com.example.hud.standard.core.designsystem.theme.AppTheme
import com.example.hud.standard.core.flags.FeatureFlags
import com.example.hud.standard.core.flags.Flag
import com.example.hud.standard.core.flags.FlagOverrides

/**
 * Every declared flag with the value the app reads now. A switch overrides a yes/no flag for this
 * session, over whatever the source says; Reset returns it to the source.
 */
@Composable
internal fun FlagsTab(flags: FeatureFlags, overrides: FlagOverrides) {
    val overridden by overrides.changes.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        items(
            items = Flag.ALL,
            key = { it.key },
            contentType = { if (it is Flag.Bool) SwitchRowType else ValueRowType },
        ) { flag ->
            FlagRow(
                flag = flag,
                value = flags[flag],
                isOverridden = flag.key in overridden,
                onReset = { overrides.reset(flag) },
                onToggle = (flag as? Flag.Bool)?.let { bool -> { checked: Boolean -> overrides.set(bool, checked) } },
            )
        }
    }
}

@Composable
private fun FlagRow(
    flag: Flag<*>,
    value: Any?,
    isOverridden: Boolean,
    onReset: () -> Unit,
    onToggle: ((Boolean) -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        AppText(
            text = flag.key,
            style = AppTheme.typography.bodyMedium,
            color = if (isOverridden) AppTheme.colors.warning.content else AppTheme.colors.contentPrimary,
            modifier = Modifier.weight(1f),
        )
        if (isOverridden) {
            AppButton(text = "Reset", onClick = onReset, variant = ButtonVariant.Ghost, size = ButtonSize.Small)
        }
        if (onToggle != null) {
            AppSwitch(checked = value == true, onCheckedChange = onToggle)
        } else {
            AppMonoText(text = value.toString(), color = AppTheme.colors.contentSecondary, maxLines = 1)
        }
    }
}

private const val SwitchRowType = "switch"
private const val ValueRowType = "value"
