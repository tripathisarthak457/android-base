package com.base.app.core.designsystem.component.datetime

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.appClickable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.foundation.rememberCurrentLocale
import com.base.app.core.designsystem.theme.AppTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * A month calendar.
 *
 * ## The week starts where the user's locale says it starts
 *
 * Read from `WeekFields.of(Locale.getDefault())` rather than hardcoded to Monday or Sunday. A
 * calendar that starts on the wrong day is not a cosmetic problem — people read the position of a
 * date in the grid, and a shifted grid produces genuinely wrong answers about what day something
 * falls on.
 *
 * ## Selectable dates are a predicate
 *
 * [minDate] and [maxDate] cover the common bounds, and [isDateEnabled] covers everything else —
 * no weekends, no public holidays, only dates a delivery slot exists for. A component that only
 * understood a range would need a second component the first time a real rule appeared.
 *
 * ## Month changes slide in the direction of travel
 *
 * Forwards enters from the right, backwards from the left. Cross-fading loses the one piece of
 * information the animation could carry, which is which way in time you just moved.
 */
@Composable
fun AppDatePicker(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isDateEnabled: (LocalDate) -> Boolean = { true },
) {
    val today = remember { LocalDate.now() }
    var visibleMonth by remember(selectedDate) {
        mutableStateOf(YearMonth.from(selectedDate ?: today))
    }
    var forwards by remember { mutableStateOf(true) }

    // Captured out of the transitionSpec below: it is not a composable scope, so the theme's
    // composable accessors cannot be read from inside it.
    val slideDuration = AppTheme.motion.medium
    val fadeDuration = AppTheme.motion.quick

    val locale = rememberCurrentLocale()
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val weekdays = remember(firstDayOfWeek, locale) {
        (0..6).map { firstDayOfWeek.plus(it.toLong()).getDisplayName(TextStyle.NARROW, locale) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        MonthHeader(
            month = visibleMonth,
            locale = locale,
            onPrevious = {
                forwards = false
                visibleMonth = visibleMonth.minusMonths(1)
            },
            onNext = {
                forwards = true
                visibleMonth = visibleMonth.plusMonths(1)
            },
        )

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.sm)) {
            weekdays.forEach { day ->
                AppText(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.contentTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        AnimatedContent(
            targetState = visibleMonth,
            transitionSpec = {
                val direction = if (forwards) 1 else -1
                (
                    slideInHorizontally(tween(slideDuration)) { it * direction } +
                        fadeIn(tween(fadeDuration))
                    ) togetherWith (
                    slideOutHorizontally(tween(slideDuration)) { -it * direction } +
                        fadeOut(tween(fadeDuration))
                    ) using SizeTransform(clip = false)
            },
            label = "monthGrid",
        ) { month ->
            MonthGrid(
                month = month,
                firstDayOfWeek = firstDayOfWeek,
                today = today,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                isSelectable = { date ->
                    (minDate == null || !date.isBefore(minDate)) &&
                        (maxDate == null || !date.isAfter(maxDate)) &&
                        isDateEnabled(date)
                },
            )
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    locale: Locale,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            icon = AppIcons.ChevronLeft,
            contentDescription = "Previous month",
            onClick = onPrevious,
            size = ButtonSize.Small,
        )
        AppText(
            text = "${month.month.getDisplayName(TextStyle.FULL, locale)} ${month.year}",
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.titleLarge,
            color = AppTheme.colors.contentPrimary,
            textAlign = TextAlign.Center,
        )
        AppIconButton(
            icon = AppIcons.ChevronRight,
            contentDescription = "Next month",
            onClick = onNext,
            size = ButtonSize.Small,
        )
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    today: LocalDate,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    isSelectable: (LocalDate) -> Boolean,
) {
    // The number of blank cells before the 1st. Adding 7 before the modulo keeps the result
    // non-negative for every locale's first day, which a bare subtraction does not.
    val leadingBlanks = remember(month, firstDayOfWeek) {
        (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + DAYS_IN_WEEK) % DAYS_IN_WEEK
    }
    val dayCount = month.lengthOfMonth()
    val rows = ((leadingBlanks + dayCount + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(DAYS_IN_WEEK) { column ->
                    val dayOfMonth = row * DAYS_IN_WEEK + column - leadingBlanks + 1
                    Box(modifier = Modifier.weight(1f)) {
                        if (dayOfMonth in 1..dayCount) {
                            val date = month.atDay(dayOfMonth)
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                enabled = isSelectable(date),
                                onClick = { onDateSelected(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors

    val background = when {
        isSelected -> colors.accent
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val content = when {
        isSelected -> colors.onAccent
        !enabled -> colors.contentDisabled
        isToday -> colors.accent
        else -> colors.contentPrimary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(AppTheme.shapes.pill)
            .background(background)
            // Today is marked with a ring, not with colour alone — the same date can be today
            // *and* be disabled, and a colour-only marker would be lost against the disabled grey.
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(AppTheme.sizes.borderWidth, colors.accent, AppTheme.shapes.pill)
                } else {
                    Modifier
                },
            )
            .appClickable(
                onClick = onClick,
                enabled = enabled,
                minTouchTarget = 0.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = date.dayOfMonth.toString(),
            style = if (isSelected || isToday) {
                AppTheme.typography.titleSmall
            } else {
                AppTheme.typography.bodySmall
            },
            color = content,
            textAlign = TextAlign.Center,
        )
    }
}

/** The picker in a dialog, with Cancel and Confirm. The common way to reach it. */
@Composable
fun AppDatePickerDialog(
    initialDate: LocalDate?,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Select date",
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isDateEnabled: (LocalDate) -> Boolean = { true },
) {
    var draft by remember { mutableStateOf(initialDate ?: LocalDate.now()) }

    com.base.app.core.designsystem.component.overlay.AppDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        AppText(
            text = title,
            style = AppTheme.typography.headingMedium,
            color = AppTheme.colors.contentPrimary,
        )

        AppDatePicker(
            selectedDate = draft,
            onDateSelected = { draft = it },
            minDate = minDate,
            maxDate = maxDate,
            isDateEnabled = isDateEnabled,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            AppButton(
                text = "Cancel",
                onClick = onDismissRequest,
                variant = ButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = "Confirm",
                onClick = {
                    onDismissRequest()
                    onConfirm(draft)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val DAYS_IN_WEEK = 7
