package com.base.app.core.designsystem.component.pickers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.animation.rememberAppTransitions
import com.base.app.core.designsystem.component.button.AppButtonDefaults
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.content.rememberCurrentLocale
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.foundation.scroll.AppScrollDefaults
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * A month calendar. Swipe sideways or use the arrows to change month; the week starts on the
 * locale's first day. A screen reader hears each day as a full date and whether it is selected.
 * [today] defaults to the device's date; pass the server's when it can differ.
 */
@Composable
public fun AppDatePicker(
    selectedDate: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    today: LocalDate = remember { LocalDate.now() },
    isDateEnabled: (LocalDate) -> Boolean = { true },
) {
    val locale = rememberCurrentLocale()
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val anchor = remember { YearMonth.from(selectedDate ?: today) }
    val pager = rememberPagerState(initialPage = MonthsEachWay) { MonthsEachWay * 2 + 1 }
    val month = anchor.plusMonths((pager.currentPage - MonthsEachWay).toLong())
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val weekdays = remember(firstDayOfWeek, locale) {
        (0..6).map { firstDayOfWeek.plus(it.toLong()).getDisplayName(TextStyle.NARROW, locale) }
    }
    val title = remember(month, locale) { month.format(DateTimeFormatter.ofPattern("LLLL y", locale)) }
    val dayFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppText(
                text = title,
                modifier = Modifier.weight(1f).padding(start = AppTheme.spacing.sm),
                style = AppTheme.typography.headline,
                color = AppTheme.colors.contentPrimary,
            )
            AppIconButton(
                icon = AppIcons.ChevronLeft,
                contentDescription = stringResource(R.string.designsystem_previous_month),
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                size = ButtonSize.Small,
                colors = AppButtonDefaults.iconButtonColors(content = AppTheme.colors.accentContent),
            )
            AppIconButton(
                icon = AppIcons.ChevronRight,
                contentDescription = stringResource(R.string.designsystem_next_month),
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                size = ButtonSize.Small,
                colors = AppButtonDefaults.iconButtonColors(content = AppTheme.colors.accentContent),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.sm)) {
            weekdays.forEach { day ->
                AppText(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.caption2,
                    color = AppTheme.colors.contentSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            flingBehavior = AppScrollDefaults.pagerFlingBehavior(pager),
        ) { page ->
            MonthGrid(
                month = anchor.plusMonths((page - MonthsEachWay).toLong()),
                firstDayOfWeek = firstDayOfWeek,
                today = today,
                selectedDate = selectedDate,
                describe = { dayFormat.format(it) },
                isSelectable = { date ->
                    (minDate == null || !date.isBefore(minDate)) && (maxDate == null || !date.isAfter(maxDate)) && isDateEnabled(date)
                },
                onDateChange = { date ->
                    if (date != selectedDate) haptics.perform(AppHaptic.Selection)
                    onDateChange(date)
                },
            )
        }
    }
}

/**
 * A date shown as a pill that opens the calendar beneath it in place, as iOS's compact date picker
 * does. Choosing a day closes it again.
 */
@Composable
public fun AppDatePill(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val locale = rememberCurrentLocale()
    val label = remember(date, locale) { date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) }
    val transitions = rememberAppTransitions()
    Column(modifier = modifier) {
        PickerPill(text = label, open = open, onClick = { open = !open })
        AnimatedVisibility(visible = open, enter = transitions.expandIn, exit = transitions.collapseOut) {
            AppDatePicker(
                selectedDate = date,
                onDateChange = {
                    onDateChange(it)
                    open = false
                },
                modifier = Modifier.padding(top = AppTheme.spacing.sm),
                minDate = minDate,
                maxDate = maxDate,
            )
        }
    }
}

/** The pill a compact picker shows its value in; it takes the accent while its picker is open. */
@Composable
internal fun PickerPill(text: String, open: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    AppText(
        text = text,
        modifier = Modifier
            .pressable(onClick = onClick, feedback = PressFeedback.Dim)
            .background(colors.fill, AppTheme.shapes.sm)
            .padding(horizontal = AppDatePickerDefaults.PillHorizontalPadding, vertical = AppDatePickerDefaults.PillVerticalPadding),
        style = AppTheme.typography.body,
        color = if (open) colors.accentContent else colors.contentPrimary,
    )
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    today: LocalDate,
    selectedDate: LocalDate?,
    describe: (LocalDate) -> String,
    isSelectable: (LocalDate) -> Boolean,
    onDateChange: (LocalDate) -> Unit,
) {
    // Blank cells before the 1st. Adding 7 before the modulo keeps it non-negative for every
    // locale's first day, which a bare subtraction does not.
    val leadingBlanks = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + DaysInWeek) % DaysInWeek
    val dayCount = month.lengthOfMonth()
    // Always six rows, so a month with five does not make the pager's height jump.
    Column(
        modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(AppDatePickerDefaults.WeekSpacing),
    ) {
        repeat(MaxRows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(DaysInWeek) { column ->
                    val day = row * DaysInWeek + column - leadingBlanks + 1
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (day in 1..dayCount) {
                            val date = month.atDay(day)
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                enabled = isSelectable(date),
                                description = describe(date),
                                onClick = { onDateChange(date) },
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
    description: String,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val content = when {
        isSelected -> colors.onAccent
        !enabled -> colors.contentDisabled
        isToday -> colors.accentContent
        else -> colors.contentPrimary
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .testTag(DayTag)
            .pressable(onClick = onClick, feedback = PressFeedback.Scale, enabled = enabled)
            .semantics {
                contentDescription = description
                selected = isSelected
            }
            .padding(AppDatePickerDefaults.DayInset)
            .background(if (isSelected) colors.accent else Color.Transparent, AppTheme.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = date.dayOfMonth.toString(),
            style = if (isSelected || isToday) AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold) else AppTheme.typography.body,
            color = content,
            textAlign = TextAlign.Center,
        )
    }
}

private const val DaysInWeek = 7
private const val MaxRows = 6

/** How far the calendar pages either way from the month it opened on: a century. */
private const val MonthsEachWay = 1200

/** The date picker's pill and day grid. */
/** A day's test tag and resource name. */
internal const val DayTag = "date_picker_day"

public object AppDatePickerDefaults {
    public val PillHorizontalPadding: Dp = 12.dp
    public val PillVerticalPadding: Dp = 7.dp
    public val WeekSpacing: Dp = 2.dp
    public val DayInset: Dp = 3.dp
}

@AppComponentPreviews
@Composable
private fun AppDatePickerPreview() {
    // A fixed day, so the screenshots do not change with the calendar.
    val today = LocalDate.of(2026, 10, 25)
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppDatePicker(
            selectedDate = today.plusDays(3),
            onDateChange = {},
            today = today,
            minDate = today,
            isDateEnabled = { it.dayOfWeek != DayOfWeek.SUNDAY },
        )
        AppDatePill(date = today, onDateChange = {})
    }
}
