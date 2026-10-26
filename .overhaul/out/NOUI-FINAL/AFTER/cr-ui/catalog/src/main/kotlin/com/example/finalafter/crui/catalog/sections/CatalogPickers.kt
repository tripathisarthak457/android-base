package com.example.finalafter.crui.catalog.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.finalafter.crui.catalog.CatalogGroup
import com.example.finalafter.crui.core.designsystem.component.pickers.AppDatePicker
import com.example.finalafter.crui.core.designsystem.component.pickers.AppDatePill
import com.example.finalafter.crui.core.designsystem.component.pickers.AppTimePicker
import com.example.finalafter.crui.core.designsystem.component.pickers.AppTimePill
import com.example.finalafter.crui.core.designsystem.component.pickers.AppWheelPicker
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun PickersSection() {
    CatalogGroup("Calendar", caption = "Swipe sideways or use the arrows; the week starts on the locale's first day.") {
        var date by remember { mutableStateOf(LocalDate.of(2026, 10, 9)) }
        AppDatePicker(selectedDate = date, onDateSelected = { date = it }, today = LocalDate.of(2026, 10, 9), minDate = LocalDate.of(2026, 1, 1))
    }
    CatalogGroup("Time", caption = "Wheels that read each other's current value when they settle, so changing the hour keeps the minutes.") {
        var time by remember { mutableStateOf(LocalTime.of(9, 30)) }
        AppTimePicker(time = time, onTimeChange = { time = it }, minuteStep = 5)
    }
    CatalogGroup("Wheel", caption = "One font throughout; the rows lean and fade as they leave the middle.") {
        var size by remember { mutableIntStateOf(2) }
        AppWheelPicker(items = listOf("XS", "S", "M", "L", "XL", "XXL"), selectedIndex = size, onSelectedChange = { size = it }, label = { it })
    }
    CatalogGroup("Compact pills", caption = "The value as a pill that opens its picker in place.") {
        var date by remember { mutableStateOf(LocalDate.of(2026, 10, 12)) }
        var time by remember { mutableStateOf(LocalTime.of(18, 0)) }
        AppDatePill(date = date, onDateChange = { date = it })
        AppTimePill(time = time, onTimeChange = { time = it }, minuteStep = 15)
    }
}
