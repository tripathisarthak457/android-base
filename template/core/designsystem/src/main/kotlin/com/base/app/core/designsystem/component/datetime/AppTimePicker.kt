package com.base.app.core.designsystem.component.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.overlay.AppDialog
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.rememberCurrentLocale
import com.base.app.core.designsystem.theme.AppTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Hour, minute and — when the locale uses one — a period, each on its own wheel.
 *
 * Wheels rather than a clock dial. A dial is charming and it is slow: setting 7:43 means two
 * separate drags with a mode switch between them, and the minute hand is fiddly at any size that
 * fits on a phone. Two wheels get there in one gesture each, and they are usable one-handed.
 *
 * ## Twelve- or twenty-four-hour is read from the locale
 *
 * Asked of `DateTimeFormatter`, not assumed. Showing a 24-hour user an AM/PM control is a
 * conversion they have to do in their head every time, and the reverse is worse — a 12-hour user
 * shown "19" will frequently misread it.
 */
@Composable
fun AppTimePicker(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    minuteStep: Int = 1,
) {
    val locale = rememberCurrentLocale()
    val use24Hour = remember(locale) { locale.uses24HourClock() }

    val hours = remember(use24Hour) { if (use24Hour) (0..23).toList() else (1..12).toList() }
    val minutes = remember(minuteStep) { (0..59 step minuteStep).toList() }
    val periods = remember { listOf("AM", "PM") }

    val hourIndex = remember(time, use24Hour) {
        if (use24Hour) {
            time.hour
        } else {
            // 0 and 12 both display as 12 on a twelve-hour clock; every other hour is its
            // remainder. hours is 1..12, so the index is one less than the displayed value.
            val displayed = if (time.hour % 12 == 0) 12 else time.hour % 12
            displayed - 1
        }
    }

    val minuteIndex = minutes.indexOfFirst { it >= time.minute }.coerceAtLeast(0)
    val periodIndex = if (time.hour < 12) 0 else 1

    fun emit(hour: Int, minute: Int, period: Int) {
        val resolvedHour = when {
            use24Hour -> hour
            period == 0 -> if (hour == 12) 0 else hour
            else -> if (hour == 12) 12 else hour + 12
        }
        onTimeChange(LocalTime.of(resolvedHour.coerceIn(0, 23), minute.coerceIn(0, 59)))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppWheelPicker(
            items = hours,
            selectedIndex = hourIndex,
            onSelectedChange = { index -> emit(hours[index], minutes[minuteIndex], periodIndex) },
            modifier = Modifier.weight(1f),
            label = { it.toString().padStart(2, '0') },
        )

        AppText(
            text = ":",
            style = AppTheme.typography.displaySmall,
            color = AppTheme.colors.contentTertiary,
            modifier = Modifier.padding(bottom = 2.dp),
        )

        AppWheelPicker(
            items = minutes,
            selectedIndex = minuteIndex,
            onSelectedChange = { index ->
                emit(hours[hourIndex], minutes[index], periodIndex)
            },
            modifier = Modifier.weight(1f),
            label = { it.toString().padStart(2, '0') },
        )

        if (!use24Hour) {
            AppWheelPicker(
                items = periods,
                selectedIndex = periodIndex,
                onSelectedChange = { index ->
                    emit(hours[hourIndex], minutes[minuteIndex], index)
                },
                modifier = Modifier.weight(1f),
                label = { it },
            )
        }
    }
}

/** The picker in a dialog, with Cancel and Confirm. */
@Composable
fun AppTimePickerDialog(
    initialTime: LocalTime,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Select time",
    minuteStep: Int = 1,
) {
    var draft by remember { mutableStateOf(initialTime) }

    AppDialog(onDismissRequest = onDismissRequest, modifier = modifier) {
        AppText(
            text = title,
            style = AppTheme.typography.headingMedium,
            color = AppTheme.colors.contentPrimary,
        )

        AppTimePicker(
            time = draft,
            onTimeChange = { draft = it },
            minuteStep = minuteStep,
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

/**
 * Whether this locale writes times on a 24-hour clock.
 *
 * Asked of the platform by formatting a sample, rather than derived from a hardcoded list of
 * countries. Country lists are wrong for the user who has set a different regional format from
 * their country's default, which is common enough to matter.
 */
private fun Locale.uses24HourClock(): Boolean {
    val sample = runCatching {
        LocalTime.of(13, 0).format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(this),
        )
    }.getOrNull() ?: return true

    // 13:00 renders as "1:00 PM" on a twelve-hour locale and "13:00" on a twenty-four-hour one.
    // The presence of any letter is the period marker, whatever language it is written in.
    return sample.none { it.isLetter() }
}
