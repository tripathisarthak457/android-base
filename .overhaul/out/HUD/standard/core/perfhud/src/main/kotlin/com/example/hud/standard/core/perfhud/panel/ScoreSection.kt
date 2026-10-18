package com.example.hud.standard.core.perfhud.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.example.hud.standard.core.designsystem.component.container.AppCard
import com.example.hud.standard.core.designsystem.component.container.AppDivider
import com.example.hud.standard.core.designsystem.component.feedback.AppStatusPill
import com.example.hud.standard.core.designsystem.component.text.AppText
import com.example.hud.standard.core.designsystem.theme.AppTheme
import com.example.hud.standard.core.designsystem.theme.StatusColors
import com.example.hud.standard.core.perfhud.R
import com.example.hud.standard.core.perfhud.score.Grade
import com.example.hud.standard.core.perfhud.score.MetricScore
import com.example.hud.standard.core.perfhud.score.OptimisationScore
import com.example.hud.standard.core.perfhud.score.decimal
import com.example.hud.standard.core.perfhud.score.doc
import com.example.hud.standard.core.perfhud.score.fix
import com.example.hud.standard.core.perfhud.score.formatTarget
import com.example.hud.standard.core.perfhud.score.formatValue
import com.example.hud.standard.core.perfhud.score.title

/** The score, then each lost point with what to do about it, then every metric's grade. */
@Composable
internal fun ScoreSection(score: OptimisationScore) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        AppText(
            text = stringResource(R.string.perfhud_score),
            style = AppTheme.typography.titleSmall,
            color = AppTheme.colors.contentSecondary,
        )
        if (!score.measured) {
            AppText(
                text = stringResource(R.string.perfhud_score_none),
                style = AppTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = AppTheme.spacing.sm),
            )
            return@AppCard
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            modifier = Modifier.padding(vertical = AppTheme.spacing.sm),
        ) {
            AppText(
                text = score.total.toString(),
                style = AppTheme.typography.displaySmall,
                color = AppTheme.colors.contentPrimary,
            )
            AppStatusPill(text = score.grade.name, status = score.grade.colors())
        }

        AppText(
            text = stringResource(R.string.perfhud_losses),
            style = AppTheme.typography.titleSmall,
            modifier = Modifier.padding(top = AppTheme.spacing.sm),
        )
        val losses = score.losses
        if (losses.isEmpty()) {
            AppText(text = stringResource(R.string.perfhud_no_losses), style = AppTheme.typography.bodySmall)
        }
        losses.forEach { lost -> Loss(lost) }

        AppDivider(modifier = Modifier.padding(vertical = AppTheme.spacing.md))
        score.metrics.forEach { scored -> MetricRow(scored) }
    }
}

@Composable
private fun Loss(scored: MetricScore) {
    val resources = LocalResources.current
    val metric = scored.metric
    Column(
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        modifier = Modifier.padding(top = AppTheme.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(
                text = stringResource(metric.title),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.contentPrimary,
                modifier = Modifier.weight(1f),
            )
            AppText(
                text = stringResource(R.string.perfhud_lost_points, decimal(scored.lost, 1), metric.weight),
                style = AppTheme.typography.labelSmall,
                color = AppTheme.colors.danger.content,
            )
        }
        val value = scored.value
        if (value != null) {
            AppText(
                text = stringResource(
                    R.string.perfhud_metric_line,
                    stringResource(metric.title),
                    resources.formatValue(metric, value),
                    resources.formatTarget(metric),
                ),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.contentSecondary,
            )
        }
        AppText(text = stringResource(metric.fix), style = AppTheme.typography.bodySmall)
        AppText(
            text = stringResource(R.string.perfhud_doc, metric.doc),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.contentTertiary,
        )
    }
}

@Composable
private fun MetricRow(scored: MetricScore) {
    val resources = LocalResources.current
    val value = scored.value
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        modifier = Modifier.padding(vertical = AppTheme.spacing.xxs),
    ) {
        AppText(
            text = stringResource(scored.metric.title),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentSecondary,
            modifier = Modifier.weight(1f),
        )
        AppText(
            text = if (value == null) {
                stringResource(R.string.perfhud_not_measured)
            } else {
                resources.formatValue(scored.metric, value)
            },
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentPrimary,
        )
        val grade = scored.grade
        if (grade != null) AppStatusPill(text = grade.name, status = grade.colors())
    }
}

@Composable
private fun Grade.colors(): StatusColors = when (this) {
    Grade.A -> AppTheme.colors.success
    Grade.B -> AppTheme.colors.info
    Grade.C, Grade.D -> AppTheme.colors.warning
    Grade.F -> AppTheme.colors.danger
}
