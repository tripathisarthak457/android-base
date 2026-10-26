package com.example.finalafter.crui.core.perfhud.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.finalafter.crui.core.designsystem.component.container.AppCard
import com.example.finalafter.crui.core.designsystem.component.container.AppDivider
import com.example.finalafter.crui.core.designsystem.component.feedback.AppStatusPill
import com.example.finalafter.crui.core.designsystem.component.text.AppText
import com.example.finalafter.crui.core.designsystem.theme.AppTheme
import com.example.finalafter.crui.core.designsystem.theme.StatusColors
import com.example.finalafter.crui.core.perfhud.R
import com.example.finalafter.crui.core.perfhud.score.Grade
import com.example.finalafter.crui.core.perfhud.score.Metric
import com.example.finalafter.crui.core.perfhud.score.MetricScore
import com.example.finalafter.crui.core.perfhud.score.OptimisationScore
import com.example.finalafter.crui.core.perfhud.score.decimal
import com.example.finalafter.crui.core.perfhud.score.doc
import com.example.finalafter.crui.core.perfhud.score.fix
import com.example.finalafter.crui.core.perfhud.score.formatTarget
import com.example.finalafter.crui.core.perfhud.score.formatValue
import com.example.finalafter.crui.core.perfhud.score.title

/** The score, then each lost point with what to do about it, then every metric's grade. */
@Composable
internal fun ScoreSection(score: OptimisationScore) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        AppText(
            text = stringResource(R.string.perfhud_score),
            style = AppTheme.typography.footnote,
            color = AppTheme.colors.contentSecondary,
        )
        val total = score.total
        val grade = score.grade
        if (total == null || grade == null) {
            AppText(
                text = pluralStringResource(R.plurals.perfhud_score_none, Metric.MIN_FRAMES, Metric.MIN_FRAMES),
                style = AppTheme.typography.callout,
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
                text = total.toString(),
                style = AppTheme.typography.title2,
                color = AppTheme.colors.contentPrimary,
            )
            AppStatusPill(text = grade.name, status = grade.colors())
        }

        AppText(
            text = stringResource(R.string.perfhud_losses),
            style = AppTheme.typography.footnote,
            modifier = Modifier.padding(top = AppTheme.spacing.sm),
        )
        val losses = score.losses
        if (losses.isEmpty()) {
            AppText(text = stringResource(R.string.perfhud_no_losses), style = AppTheme.typography.subheadline)
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
                style = AppTheme.typography.callout,
                color = AppTheme.colors.contentPrimary,
                modifier = Modifier.weight(1f),
            )
            AppText(
                text = stringResource(R.string.perfhud_lost_points, decimal(scored.lost, 1), metric.weight),
                style = AppTheme.typography.caption2,
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
                style = AppTheme.typography.subheadline,
                color = AppTheme.colors.contentSecondary,
            )
        }
        AppText(text = stringResource(metric.fix), style = AppTheme.typography.subheadline)
        AppText(
            text = stringResource(R.string.perfhud_doc, metric.doc),
            style = AppTheme.typography.caption1,
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
            style = AppTheme.typography.subheadline,
            color = AppTheme.colors.contentSecondary,
            modifier = Modifier.weight(1f),
        )
        AppText(
            text = if (value == null) {
                stringResource(R.string.perfhud_not_measured)
            } else {
                resources.formatValue(scored.metric, value)
            },
            style = AppTheme.typography.subheadline,
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
