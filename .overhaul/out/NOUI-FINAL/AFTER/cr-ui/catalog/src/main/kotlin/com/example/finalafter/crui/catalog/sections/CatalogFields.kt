package com.example.finalafter.crui.catalog.sections

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.finalafter.crui.catalog.CatalogGroup
import com.example.finalafter.crui.core.designsystem.component.formfields.AppDecimalField
import com.example.finalafter.crui.core.designsystem.component.formfields.AppMoneyField
import com.example.finalafter.crui.core.designsystem.component.formfields.AppOtpField
import com.example.finalafter.crui.core.designsystem.component.formfields.AppPhoneField
import com.example.finalafter.crui.core.designsystem.component.formfields.AppRangeSlider
import com.example.finalafter.crui.core.designsystem.component.formfields.AppSelectField
import com.example.finalafter.crui.core.designsystem.component.formfields.AppStepIndicator
import com.example.finalafter.crui.core.designsystem.component.formfields.AppStepper
import com.example.finalafter.crui.core.designsystem.component.formfields.AppTagField
import com.example.finalafter.crui.core.designsystem.component.text.AppFiguresText
import com.example.finalafter.crui.core.designsystem.theme.AppTheme

@Composable
fun FieldsSection() {
    CatalogGroup("One-time code", caption = "One real field under the boxes, so paste and SMS autofill land at once.") {
        AppOtpField(state = rememberTextFieldState(), label = "Verification code", autoFocus = false)
    }
    CatalogGroup("Decimal and money", caption = "Grouping is shown in the locale's style but never stored, and the caret stays where it was typed.") {
        AppDecimalField(state = rememberTextFieldState("1250.5"), label = "Distance", unit = "km")
        var cents by remember { mutableStateOf<Long?>(129_999) }
        AppMoneyField(minorUnits = cents, onMinorUnitsChange = { cents = it }, label = "Amount")
        AppFiguresText(text = "Stored: ${cents ?: "empty"}", style = AppTheme.typography.footnote, color = AppTheme.colors.contentSecondary)
    }
    CatalogGroup("Phone, select and tags") {
        var code by remember { mutableStateOf("+1") }
        AppPhoneField(dialCode = code, onDialCodeChange = { code = it }, number = rememberTextFieldState())
        var city by remember { mutableStateOf<String?>(null) }
        AppSelectField(value = city, options = listOf("Lisbon", "Oslo", "Seoul", "Toronto"), onSelect = { city = it }, label = "City")
        var tags by remember { mutableStateOf(listOf("design", "ios")) }
        AppTagField(tags = tags, onTagsChange = { tags = it }, input = rememberTextFieldState(), label = "Tags", maxTags = 5)
    }
    CatalogGroup("Stepper and range") {
        var count by remember { mutableIntStateOf(2) }
        AppStepper(value = count, onValueChange = { count = it }, range = 0..10)
        var range by remember { mutableStateOf(20f..80f) }
        AppRangeSlider(value = range, onValueChange = { range = it }, valueRange = 0f..100f)
    }
    CatalogGroup("Steps") {
        AppStepIndicator(steps = listOf("Cart", "Address", "Payment", "Done"), currentStep = 2)
    }
}
