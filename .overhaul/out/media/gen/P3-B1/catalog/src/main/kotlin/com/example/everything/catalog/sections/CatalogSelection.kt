package com.example.everything.catalog.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.everything.catalog.CatalogGroup
import com.example.everything.core.designsystem.component.selection.AppCheckbox
import com.example.everything.core.designsystem.component.selection.AppChip
import com.example.everything.core.designsystem.component.selection.AppRadio
import com.example.everything.core.designsystem.component.selection.AppSegmentedControl
import com.example.everything.core.designsystem.component.selection.AppSlider
import com.example.everything.core.designsystem.component.selection.AppSwitch
import com.example.everything.core.designsystem.component.text.AppFiguresText
import com.example.everything.core.designsystem.icon.AppIcons
import com.example.everything.core.designsystem.theme.AppTheme

@Composable
fun SelectionSection() {
    CatalogGroup("Switch", caption = "The thumb widens while held and can be dragged across.") {
        var on by remember { mutableStateOf(true) }
        Wrap {
            AppSwitch(checked = on, onCheckedChange = { on = it })
            AppSwitch(checked = !on, onCheckedChange = { on = !it })
            AppSwitch(checked = true, onCheckedChange = {}, enabled = false)
            AppSwitch(checked = false, onCheckedChange = null)
        }
    }
    CatalogGroup("Checkbox and radio", caption = "The label is part of the target. Without a callback a control only shows its state.") {
        var agreed by remember { mutableStateOf(false) }
        var choice by remember { mutableIntStateOf(0) }
        AppCheckbox(checked = agreed, onCheckedChange = { agreed = it }, label = "I agree to the terms")
        AppCheckbox(checked = true, onCheckedChange = {}, label = "Disabled", enabled = false)
        listOf("Standard delivery", "Express", "Collect in store").forEachIndexed { index, label ->
            AppRadio(selected = choice == index, onClick = { choice = index }, label = label)
        }
    }
    CatalogGroup("Segmented control", caption = "A raised thumb on a sunken track. Drag the thumb across.") {
        var period by remember { mutableIntStateOf(1) }
        AppSegmentedControl(options = listOf("Day", "Week", "Month"), selected = period, onSelect = { period = it })
        AppSegmentedControl(options = listOf("On", "Off"), selected = 0, onSelect = {}, enabled = false)
    }
    CatalogGroup("Chips", caption = "A removable chip's cross is its own target.") {
        var selected by remember { mutableStateOf(setOf("Design")) }
        Wrap {
            listOf("Design", "Engineering", "Research").forEach { tag ->
                AppChip(label = tag, onClick = { selected = if (tag in selected) selected - tag else selected + tag }, selected = tag in selected)
            }
            AppChip(label = "With icon", onClick = {}, leadingIcon = AppIcons.Star)
            AppChip(label = "Removable", onClick = {}, onRemove = {})
            AppChip(label = "Disabled", onClick = {}, enabled = false)
        }
    }
    CatalogGroup("Slider", caption = "The value follows the finger's position; a stepped slider moves step by step, and starting a scroll over it changes nothing.") {
        var volume by remember { mutableFloatStateOf(0.4f) }
        var rating by remember { mutableFloatStateOf(3f) }
        AppSlider(value = volume, onValueChange = { volume = it })
        AppSlider(value = rating, onValueChange = { rating = it }, valueRange = 1f..5f, steps = 3)
        AppFiguresText(text = "%.2f · %.0f".format(volume, rating), style = AppTheme.typography.footnote, color = AppTheme.colors.contentSecondary)
        AppSlider(value = 0.7f, onValueChange = {}, enabled = false)
    }
}
