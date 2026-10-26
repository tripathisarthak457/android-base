package com.example.cmp.core.designsystem.component.pickers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cmp.core.designsystem.animation.rememberAppTransitions
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.foundation.content.rememberCurrentLocale
import com.example.cmp.core.designsystem.preview.AppComponentPreviews
import com.example.cmp.core.designsystem.theme.AppTheme
import java.text.DateFormatSymbols
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Hour, minute and, where the locale writes one, a period, each on its own wheel. The period's names
 * come from the locale, not from English.
 */
@Composable
public fun AppTimePicker(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    minuteStep: Int = 1,
) {
    val locale = rememberCurrentLocale()
    val use24Hour = remember(locale) { locale.uses24HourClock() }
    val hours = remember(use24Hour) { if (use24Hour) (0..23).toList() else (1..12).toList() }
    val minutes = remember(minuteStep) { (0..59 step minuteStep).toList() }
    val periods = remember(locale) { DateFormatSymbols.getInstance(locale).amPmStrings.toList() }
    // Each wheel builds the new time from the others' current values, read when it settles.
    val current by rememberUpdatedState(time)

    // 0 and 12 both show as 12 on a twelve-hour clock; hours is 1..12, so the index is one less.
    fun hourIndex(t: LocalTime) = if (use24Hour) t.hour else (if (t.hour % 12 == 0) 12 else t.hour % 12) - 1
    fun minuteIndex(t: LocalTime) = minutes.indexOfFirst { it >= t.minute }.coerceAtLeast(0)
    fun periodIndex(t: LocalTime) = if (t.hour < 12) 0 else 1

    fun emit(hourAt: Int = hourIndex(current), minuteAt: Int = minuteIndex(current), periodAt: Int = periodIndex(current)) {
        val hour = hours[hourAt]
        val resolved = when {
            use24Hour -> hour
            periodAt == 0 -> if (hour == 12) 0 else hour
            else -> if (hour == 12) 12 else hour + 12
        }
        onTimeChange(LocalTime.of(resolved.coerceIn(0, 23), minutes[minuteAt].coerceIn(0, 59)))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppWheelPicker(
            items = hours,
            selectedIndex = hourIndex(time),
            onSelectedChange = { emit(hourAt = it) },
            modifier = Modifier.weight(1f),
            label = { it.toString().padStart(2, '0') },
        )
        AppText(text = ":", style = AppTheme.typography.title2, color = AppTheme.colors.contentSecondary, modifier = Modifier.padding(bottom = AppTimePickerDefaults.SeparatorLift))
        AppWheelPicker(
            items = minutes,
            selectedIndex = minuteIndex(time),
            onSelectedChange = { emit(minuteAt = it) },
            modifier = Modifier.weight(1f),
            label = { it.toString().padStart(2, '0') },
        )
        if (!use24Hour) {
            AppWheelPicker(
                items = periods,
                selectedIndex = periodIndex(time),
                onSelectedChange = { emit(periodAt = it) },
                modifier = Modifier.weight(1f),
                label = { it },
            )
        }
    }
}

/** A time shown as a pill that opens the wheels beneath it in place, as iOS's compact picker does. */
@Composable
public fun AppTimePill(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    minuteStep: Int = 1,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val locale = rememberCurrentLocale()
    val label = remember(time, locale) { time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)) }
    val transitions = rememberAppTransitions()
    Column(modifier = modifier) {
        PickerPill(text = label, open = open, onClick = { open = !open })
        AnimatedVisibility(visible = open, enter = transitions.expandIn, exit = transitions.collapseOut) {
            AppTimePicker(time = time, onTimeChange = onTimeChange, minuteStep = minuteStep, modifier = Modifier.padding(top = AppTheme.spacing.sm))
        }
    }
}

/** Whether this locale writes times on a 24-hour clock. */
private fun Locale.uses24HourClock(): Boolean {
    val sample = runCatching {
        LocalTime.of(13, 0).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(this))
    }.getOrNull() ?: return true
    // 13:00 renders as "1:00 PM" on a twelve-hour locale and "13:00" on a twenty-four-hour one;
    // any letter is the period marker, whatever language it is in.
    return sample.none { it.isLetter() }
}

/** The time picker's colon, set on the digits' baseline. */
public object AppTimePickerDefaults {
    public val SeparatorLift: Dp = 2.dp
}

@AppComponentPreviews
@Composable
private fun AppTimePickerPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTimePicker(time = LocalTime.of(9, 30), onTimeChange = {})
        AppTimePicker(time = LocalTime.of(18, 45), onTimeChange = {}, minuteStep = 15)
        AppTimePill(time = LocalTime.of(7, 5), onTimeChange = {})
    }
}
